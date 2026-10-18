import os
from PIL import Image, ImageDraw, ImageFilter

src_path = r"C:/Users/thisg/.gemini/antigravity/brain/88c27407-671c-4225-90f9-6b8751fc17be/.user_uploaded/media_1792284240247_6af3701a.png"
res_dir = r"C:/My/code/GalaxyApp/app/src/main/res"

img = Image.open(src_path).convert("RGBA")
w, h = img.size

# Square crop centered on the face
min_dim = min(w, h)
left = (w - min_dim) // 2
top = max(0, int((h - min_dim) * 0.35)) # slightly biased towards top to center face
right = left + min_dim
bottom = top + min_dim

cropped = img.crop((left, top, right, bottom))
base_512 = cropped.resize((512, 512), Image.Resampling.LANCZOS)

# Create circular icon with a smooth circular mask
def make_circular(im, size):
    im_resized = im.resize((size, size), Image.Resampling.LANCZOS)
    mask = Image.new("L", (size * 4, size * 4), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    mask = mask.resize((size, size), Image.Resampling.LANCZOS)
    
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(im_resized, (0, 0), mask=mask)
    return out

# Create rounded square icon
def make_rounded(im, size):
    im_resized = im.resize((size, size), Image.Resampling.LANCZOS)
    mask = Image.new("L", (size * 4, size * 4), 0)
    draw = ImageDraw.Draw(mask)
    radius = int(size * 4 * 0.22)
    draw.rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=radius, fill=255)
    mask = mask.resize((size, size), Image.Resampling.LANCZOS)
    
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(im_resized, (0, 0), mask=mask)
    return out

densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

for folder, size in densities.items():
    target_folder = os.path.join(res_dir, folder)
    os.makedirs(target_folder, exist_ok=True)
    
    # Save round icon (used by Wear OS circular launchers)
    round_icon = make_circular(base_512, size)
    round_icon.save(os.path.join(target_folder, "ic_launcher_round.png"))
    
    # Save standard icon
    std_icon = make_circular(base_512, size) # on watch, circular is standard
    std_icon.save(os.path.join(target_folder, "ic_launcher.png"))

# Save a high-res preview in the artifact directory
preview_dir = r"C:/Users/thisg/.gemini/antigravity/brain/88c27407-671c-4225-90f9-6b8751fc17be"
preview_icon = make_circular(base_512, 256)
preview_path = os.path.join(preview_dir, "icon_preview.png")
preview_icon.save(preview_path)
print(f"Successfully generated all mipmap icons and preview at {preview_path}")
