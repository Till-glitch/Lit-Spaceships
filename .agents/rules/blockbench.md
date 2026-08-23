# Blockbench Minecraft Modeling & Texturing Guidelines

## Fokus
Dieses Projekt nutzt Blockbench ausschließlich für Minecraft-spezifische Modelle (Java Block/Item JSONs, Entity Geo-Modelle) und Pixelart-Texturen.

## Erlaubte & aktive Tool-Kategorien
- **Cuboid Modeling**: `place_cube`, `modify_cube`, `remove_element`, `duplicate_element`, `add_group`, `rename_element`, `list_outline`, `get_selection`.
- **Texturing & Painting**: `create_texture`, `apply_texture`, `add_texture_group`, `list_textures`, `get_texture`, `activate_texture`, `paint_fill_tool`, `draw_shape_tool`, `gradient_tool`, `color_picker_tool`, `copy_brush_tool`, `eraser_tool`, `paint_settings`, `paint_with_brush`, `texture_layer_management`.
- **Projekt-Export & Kamera**: `create_project`, `get_project_info`, `export_model`, `list_export_formats`, `from_geo_json`, `save_checkpoint`, `set_camera_angle`, `capture_screenshot`.

## Deaktivierte / Nicht-relevante Features
- Vertex-Weighting / Skelettrigging (Armatures)
- Komplexe Animations-Timeline & Graph-Editor
- PBR-Shader-Materialien & Material-Instanzen
- Freiform-Polygon-Meshes (Zylinder/Sphären/Mesh-Subdivide/Knife)
- UI-Click-Emulation & Script-Evaluation
