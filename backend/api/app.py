from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename
from pathlib import Path
from datetime import datetime
import tempfile
import shutil
import traceback

from main_from_image import run_image
from auth_utils import firebase_token_required
import firebase_config  # initializes Firebase

app = Flask(__name__)

# Save folder
SAVE_DIR = Path("fen_chess_data/data")
SAVE_DIR.mkdir(parents=True, exist_ok=True)

@app.route("/detect_fen", methods=["POST"])
@firebase_token_required
def detect_fen():
    if 'image' not in request.files:
        return jsonify({"error": "No image file provided"}), 400

    image_file = request.files['image']

    try:
        # Save original image with timestamp
        ext = Path(image_file.filename).suffix
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = secure_filename(f"input_{timestamp}{ext}")
        saved_path = SAVE_DIR / filename
        image_file.save(saved_path)

        # Temp file for processing
        _, tmp_path_str = tempfile.mkstemp(suffix=ext)
        tmp_path = Path(tmp_path_str)
        shutil.copy(saved_path, tmp_path)

        # Run model
        fen = run_image(str(tmp_path))
        return jsonify({"fen": fen})

    except Exception as e:
        traceback.print_exc()
        return jsonify({"error": str(e)}), 500

    finally:
        if tmp_path.exists():
            tmp_path.unlink(missing_ok=True)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000)
