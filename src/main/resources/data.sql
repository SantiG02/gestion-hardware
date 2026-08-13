/*INSERT INTO sede (nombre, direccion, ciudad, activo) VALUES
('Sede Principal', 'Calle 100 #15-20', 'Bogota', true),
('Bodega Norte', 'Autopista Norte km 18', 'Bogota', true);

INSERT INTO area (sede_id, nombre, activo) VALUES
(1, 'Contabilidad', true),
(1, 'Sistemas', true),
(1, 'Gerencia', true),
(2, 'Almacen', true);

INSERT INTO tipo_equipo (nombre) VALUES
('Escritorio'), ('Portatil'), ('Servidor'), ('Impresora');

INSERT INTO estado_equipo (nombre, descripcion, color_hex, orden) VALUES
('Estable', 'Sin fallas recurrentes', '#1D9E75', 1),
('En seguimiento', 'Presenta fallas frecuentes', '#C88A12', 2),
('Candidato a renovacion', 'Supera umbrales criticos', '#C0392B', 3);*/

INSERT INTO equipo (codigo_interno, serial, tipo_equipo_id, area_id, estado_equipo_id,
                    marca, modelo, ram_gb, fecha_compra, activo) VALUES
('INV-0001', 'SN-ABC123', 2, 1, 1, 'Dell', 'Latitude 5420', 16, '2022-03-15', true),
('INV-0002', 'SN-DEF456', 1, 2, 2, 'HP', 'ProDesk 400', 8, '2020-08-10', true),
('INV-0003', 'SN-GHI789', 2, 3, 3, 'Lenovo', 'ThinkPad T480', 8, '2018-05-22', true);