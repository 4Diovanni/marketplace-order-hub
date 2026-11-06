-- Produtos fictícios para testar a API localmente.
INSERT INTO products (sku, name, price, stock_quantity, created_at, updated_at) VALUES
    ('CAM-001', 'Camiseta básica preta',        59.90, 50, NOW(6), NOW(6)),
    ('CAM-002', 'Camiseta básica branca',       59.90, 40, NOW(6), NOW(6)),
    ('CAL-001', 'Calça jeans reta',            149.90, 25, NOW(6), NOW(6)),
    ('TEN-001', 'Tênis de corrida',            299.90, 15, NOW(6), NOW(6)),
    ('MOC-001', 'Mochila 20 litros',           189.90, 10, NOW(6), NOW(6)),
    ('GAR-001', 'Garrafa térmica 500 ml',       79.90, 30, NOW(6), NOW(6)),
    ('FON-001', 'Fone de ouvido com fio',       49.90, 60, NOW(6), NOW(6)),
    ('CAR-001', 'Carregador USB-C 20 W',        89.90, 35, NOW(6), NOW(6)),
    ('LIV-001', 'Caderno universitário 10 matérias', 34.90, 80, NOW(6), NOW(6)),
    ('BON-001', 'Boné aba curva',               69.90,  0, NOW(6), NOW(6));
