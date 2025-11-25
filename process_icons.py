import os
from PIL import Image

# Configuration
SOURCE_IMAGE = "/Users/jaydeepkubavat/.gemini/antigravity/brain/00a0f02f-1da2-4d22-869e-402110668d8e/uploaded_image_0_1764073925192.png"
BANNER_SOURCE_IMAGE = "/Users/jaydeepkubavat/.gemini/antigravity/brain/00a0f02f-1da2-4d22-869e-402110668d8e/uploaded_image_0_1764074134255.png"
PROJECT_ROOT = "/Users/jaydeepkubavat/Downloads/SP-Kiosk-Bugs Fixed/app/src/main/res"

ICON_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

BANNER_SIZE = (320, 180)
BANNER_DIR = "drawable-xhdpi"

def generate_icons():
    if not os.path.exists(SOURCE_IMAGE):
        print(f"Error: Source image not found at {SOURCE_IMAGE}")
        return

    try:
        img = Image.open(SOURCE_IMAGE)
        # Ensure image is square for icons
        
        # Generate Icons
        for folder, size in ICON_SIZES.items():
            out_dir = os.path.join(PROJECT_ROOT, folder)
            os.makedirs(out_dir, exist_ok=True)
            
            # Resize
            icon = img.resize((size, size), Image.Resampling.LANCZOS)
            
            # Save as ic_launcher.png
            icon.save(os.path.join(out_dir, "ic_launcher.png"))
            
            # Save as ic_launcher_round.png (using same image for now, usually masked)
            icon.save(os.path.join(out_dir, "ic_launcher_round.png"))
            
            print(f"Generated {folder} icon ({size}x{size})")

        # Generate TV Banner
        banner_dir = os.path.join(PROJECT_ROOT, BANNER_DIR)
        os.makedirs(banner_dir, exist_ok=True)
        
        if os.path.exists(BANNER_SOURCE_IMAGE):
            banner_img = Image.open(BANNER_SOURCE_IMAGE)
        else:
            banner_img = img

        # Create banner background (using a dominant color or white/black)
        # Here we'll use a dark background as requested/standard for TV
        banner = Image.new('RGBA', BANNER_SIZE, (30, 30, 30, 255))
        
        # Resize logo to fit within banner height (leaving padding)
        target_height = 120
        aspect_ratio = banner_img.width / banner_img.height
        target_width = int(target_height * aspect_ratio)
        
        logo_resized = banner_img.resize((target_width, target_height), Image.Resampling.LANCZOS)
        
        # Center logo
        x = (BANNER_SIZE[0] - target_width) // 2
        y = (BANNER_SIZE[1] - target_height) // 2
        
        banner.paste(logo_resized, (x, y), logo_resized if logo_resized.mode == 'RGBA' else None)
        banner.save(os.path.join(banner_dir, "tv_banner.png"))
        print(f"Generated TV Banner ({BANNER_SIZE[0]}x{BANNER_SIZE[1]})")

        # Generate Play Store Icon (512x512)
        play_store_icon_size = 512
        play_store_icon = img.resize((play_store_icon_size, play_store_icon_size), Image.Resampling.LANCZOS)
        play_store_icon_path = os.path.join(PROJECT_ROOT, "../ic_launcher-playstore.png")
        play_store_icon.save(play_store_icon_path)
        print(f"Generated Play Store Icon ({play_store_icon_size}x{play_store_icon_size})")

    except Exception as e:
        print(f"Failed to process images: {e}")

if __name__ == "__main__":
    generate_icons()
