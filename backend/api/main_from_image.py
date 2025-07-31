#!/usr/bin/env python3
"""
chess_fen_from_image.py
-----------------------
Detect a chessboard in a still image, classify the 64 squares,
and print the FEN produced by your CNN.

Requirements
------------
• OpenCV (cv2) built with DNN
• TensorFlow / Keras
• Numpy
• The helper module `fen_chess_data.utils`
• Your trained files:
    YOLO_files/yolov4-tiny-obj_best.weights
    YOLO_files/yolov4-tiny-obj.cfg
    YOLO_files/obj.names
    fen_chess_data/models/model_best.h5
"""

import argparse
import os
import time
from pathlib import Path

import cv2
import numpy as np
import tensorflow as tf
from keras.losses import CategoricalCrossentropy

from fen_chess_data.utils import (
    distance, find_max_contour_area, find_outer_corners,
    do_perspective_transform, split_chessboard, preds_to_fen
)

# ──────────────────────────────────────────────────────────────────────────────
#  Model + YOLO initialisation
# ──────────────────────────────────────────────────────────────────────────────
CFG_DIR     = Path("YOLO_files")
MODEL_DIR   = Path("fen_chess_data/models")
WEIGHTS     = CFG_DIR / "yolov4-tiny-obj_best.weights"
CFG         = CFG_DIR / "yolov4-tiny-obj.cfg"
NAMES       = CFG_DIR / "obj.names"
CLASSIFIER  = MODEL_DIR / "model_best.h5"

# Hyper-params from your original notebook
CONFIDENCE_THR = 0.20
NMS_THR        = 0.30
WH_ADJUST      = 1.20           # enlarge YOLO box a little
YOLO_IN_SIZE   = 128            # the down-scaled side length

# ── classifier ───────────────────────────────────────────────────────────────
classifier = tf.keras.models.load_model(CLASSIFIER, compile=False)
classifier.compile(optimizer="adam",
                   loss=CategoricalCrossentropy(reduction="sum"))

# Mapping "class index ➜ FEN letter"
# Correct mapping: CNN index 0-12  ➜  FEN letter
IDX_TO_PIECE = [
    '',            # 0  = empty square
    'P', 'R', 'N', 'B', 'Q', 'K',   # 1–6  white pieces
    'p', 'r', 'n', 'b', 'q', 'k'    # 7–12 black pieces
]


# Convenient list of board coordinates in the same order as split_chessboard()
SQUARES = [f"{file}{rank}"
           for rank in range(8, 0, -1)         # 8 … 1
           for file in "abcdefgh"]              # a … h

# ── YOLO network ─────────────────────────────────────────────────────────────
net = cv2.dnn.readNet(str(WEIGHTS), str(CFG))
net.setPreferableBackend(cv2.dnn.DNN_BACKEND_OPENCV)
net.setPreferableTarget(cv2.dnn.DNN_TARGET_CPU)

with open(NAMES) as f:
    class_names = [ln.strip() for ln in f]

layer_names   = net.getLayerNames()
output_layers = [layer_names[i - 1] for i in net.getUnconnectedOutLayers()]

# ──────────────────────────────────────────────────────────────────────────────
#  Utility: ensure we have debug folders
# ──────────────────────────────────────────────────────────────────────────────
DBG_DIR = Path("fen_chess_data/predictions")
DBG_DIR.mkdir(parents=True, exist_ok=True)


# ──────────────────────────────────────────────────────────────────────────────
#  Core routine
# ──────────────────────────────────────────────────────────────────────────────
def run_image(
        img_path: str,
        max_side: int | None = 1200,
) -> str:
    """
    Detect a chessboard in a still image, classify the 64 squares
    with the pretrained CNN, and return the resulting FEN string.

    Parameters
    ----------
    img_path : str
        Path to the input photograph.
    max_side : int | None, default 1200
        If the longer edge of the picture is larger than this, the image is
        down-scaled before any processing.  Pass None (or 0) to disable.
    Returns
    -------
    str
        FEN describing the detected position.

    Raises
    ------
    FileNotFoundError
        If the image cannot be read.
    RuntimeError
        If no chessboard is detected.
    """
    # ── load ---------------------------------------------------------------
    frame_orig = cv2.imread(img_path)
    if frame_orig is None:
        raise FileNotFoundError(f"Could not read {img_path}")

    if max_side:
        h, w = frame_orig.shape[:2]
        long_edge = max(h, w)
        if long_edge > max_side:
            scale = max_side / long_edge
            frame_orig = cv2.resize(
                frame_orig,
                (int(w * scale), int(h * scale)),
                interpolation=cv2.INTER_AREA
            )

    # ── square-crop to the shortest edge ----------------------------------
    h0, w0, _ = frame_orig.shape
    clip = min(h0, w0)
    frame = frame_orig[:clip, :clip].copy()

    cv2.imwrite("fen_chess_data/data/test_img.jpg", frame)  # debug

    # ── prepare YOLO input -------------------------------------------------
    d = YOLO_IN_SIZE
    frame_small = cv2.resize(frame, (d, d), interpolation=cv2.INTER_AREA)
    frame_small = cv2.Canny(frame_small, d, d)
    frame_small = cv2.cvtColor(frame_small, cv2.COLOR_GRAY2BGR)

    blob = cv2.dnn.blobFromImage(
        frame_small, 0.00392, (d, d), (0, 0, 0),
        swapRB=True, crop=False
    )
    net.setInput(blob)
    outs = net.forward(output_layers)

    # ── gather detections --------------------------------------------------
    boxes, confidences, centers = [], [], []
    for out in outs:
        for det in out:
            scores   = det[5:]
            conf     = scores[np.argmax(scores)]
            if conf < CONFIDENCE_THR:
                continue

            cx, cy = int(det[0] * d), int(det[1] * d)
            w, h   = int(det[2] * d * WH_ADJUST), int(det[3] * d * WH_ADJUST)
            w = h = max(w, h)                    # force square box

            x, y = int(cx - w / 2), int(cy - h / 2)

            centers.append([cx, cy])
            boxes.append([x, y, w, h])
            confidences.append(float(conf))

    idxs = cv2.dnn.NMSBoxes(
        boxes, confidences,
        score_threshold=0.4, nms_threshold=NMS_THR
    )

    # pick the detection closest to the centre of the image
    mid = d // 2
    chosen, min_dst = None, float("inf")
    for j in range(len(centers)):
        if j not in idxs:
            continue
        dst = distance(centers[j], [mid, mid])
        if dst < min_dst:
            min_dst, chosen = dst, j

    if chosen is None:
        raise RuntimeError("No chessboard detected!")

    x, y, w, h = boxes[chosen]

    # ── map YOLO box back to original resolution --------------------------
    scale = clip / d
    x0, y0 = int(round(x * scale)), int(round(y * scale))
    w0, h0 = int(round(w * scale)), int(round(h * scale))

    board_roi = frame_orig[y0:y0 + h0, x0:x0 + w0].copy()
    cv2.imwrite(DBG_DIR / "detected_board_roi.jpg", board_roi)

    # ── perspective transform ---------------------------------------------
    gray = cv2.cvtColor(board_roi, cv2.COLOR_BGR2GRAY)
    th   = cv2.adaptiveThreshold(
        gray, 255,
        cv2.ADAPTIVE_THRESH_MEAN_C, cv2.THRESH_BINARY_INV,
        9, 3
    )
    contours, _ = cv2.findContours(
        th, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE
    )
    contours = find_max_contour_area(contours)
    peri     = cv2.arcLength(contours[0], True)
    approx   = cv2.approxPolyDP(contours[0], 0.02 * peri, True)
    pts      = find_outer_corners(board_roi, approx)

    img_warp = do_perspective_transform(board_roi, pts)
    cv2.imwrite(DBG_DIR / "warp.jpg", img_warp)

    # ── replace the tile-classification block inside run_image ────────────────
    img_pred = cv2.resize(img_warp, (256, 256), interpolation=cv2.INTER_AREA)
    img_pred = cv2.cvtColor(img_pred, cv2.COLOR_BGR2GRAY)
    img_pred = cv2.cvtColor(img_pred, cv2.COLOR_GRAY2BGR)

    tiles = split_chessboard(img_pred)                       # 64 x 32 x 32 x 3
    preds = classifier.predict(np.float32(np.array(tiles)), verbose=0)
    fen   = preds_to_fen(preds)                              # unchanged

    # Append default values for: side to move, castling, en passant, halfmove, fullmove
    full_fen = f"{fen} w KQkq - 0 1"
    return full_fen

    """
    Pretty-print a FEN position as an 8×8 board in the terminal.

    Parameters
    ----------
    fen   : str   – Forsyth-Edwards Notation (only the first field is used).
    flip  : bool  – If True, prints Black’s perspective (rank 1 at top).

    Example
    -------
    >>> print_fen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
       a  b  c  d  e  f  g  h
    8 ♜ ♞ ♝ ♛ ♚ ♝ ♞ ♜  8
    7 ♟ ♟ ♟ ♟ ♟ ♟ ♟ ♟  7
    6 · · · · · · · ·  6
    5 · · · · · · · ·  5
    4 · · · · · · · ·  4
    3 · · · · · · · ·  3
    2 ♙ ♙ ♙ ♙ ♙ ♙ ♙ ♙  2
    1 ♖ ♘ ♗ ♕ ♔ ♗ ♘ ♖  1
       a  b  c  d  e  f  g  h
    """
    print(fen)
    # Unicode chess pieces
    pieces = {
        'K': '♔', 'Q': '♕', 'R': '♖', 'B': '♗', 'N': '♘', 'P': '♙',
        'k': '♚', 'q': '♛', 'r': '♜', 'b': '♝', 'n': '♞', 'p': '♟',
    }
    board_part = fen.split()[0]
    ranks = board_part.split('/')
    if flip:
        ranks = ranks[::-1]

    files = ['a','b','c','d','e','f','g','h']
    if flip:
        files = files[::-1]

    # top coordinates
    print('   ' + '  '.join(files))
    for rank_idx, rank in enumerate(ranks, start=1):
        row = []
        for ch in rank:
            if ch.isdigit():
                row.extend(['·'] * int(ch))  # empty squares
            else:
                row.append(pieces.get(ch, '?'))
        if flip:
            row = row[::-1]
        rank_num = 8 - rank_idx + 1 if not flip else rank_idx
        print(rank_num, ' '.join(row), rank_num)
    # bottom coordinates
    print('   ' + '  '.join(files))