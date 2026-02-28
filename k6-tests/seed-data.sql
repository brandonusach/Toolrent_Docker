-- ═══════════════════════════════════════════════════════════════════════
-- SEED DE DATOS PARA PRUEBAS DE RENDIMIENTO K6
-- ToolRent - Épica 2 (Préstamos) y Épica 6 (Kardex)
--
-- Uso:
--   psql -U postgres -d toolrent_db -f seed-data.sql
--
-- Este script inserta datos de prueba en distintos volúmenes.
-- ADVERTENCIA: Solo ejecutar en entorno de pruebas/desarrollo.
-- ═══════════════════════════════════════════════════════════════════════

-- ─── 1. Verificar tablas existentes ──────────────────────────────────────
DO $$
BEGIN
    RAISE NOTICE 'Iniciando seed de datos para pruebas de rendimiento...';
END $$;

-- ─── 2. Insertar cliente de prueba (si no existe) ─────────────────────
INSERT INTO clients (id, name, rut, email, phone, status, address, created_at)
VALUES (
    1,
    'Cliente Prueba K6',
    '12345678-9',
    'test.k6@toolrent.cl',
    '+56912345678',
    'ACTIVE',
    'Av. Test 123, Santiago',
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    name   = EXCLUDED.name,
    status = EXCLUDED.status;

-- ─── 3. Insertar categoría de prueba (si no existe) ───────────────────
INSERT INTO categories (id, name, description)
VALUES (1, 'Herramientas Eléctricas', 'Categoría de prueba para K6')
ON CONFLICT (id) DO NOTHING;

-- ─── 4. Insertar herramienta de prueba (si no existe) ─────────────────
INSERT INTO tools (id, name, description, category_id, current_stock, total_stock, status, daily_rate)
VALUES (
    1,
    'Taladro de Prueba K6',
    'Herramienta de prueba para tests de rendimiento',
    1,
    100,   -- stock actual alto para no agotar durante las pruebas
    100,
    'AVAILABLE',
    5000.00
)
ON CONFLICT (id) DO UPDATE SET
    current_stock = 100,
    total_stock   = 100,
    status        = 'AVAILABLE';

-- ─── 5. Insertar préstamo de prueba base (si no existe) ───────────────
INSERT INTO loans (id, client_id, tool_id, quantity, agreed_return_date, status, notes, loan_date, created_at)
VALUES (
    1,
    1,
    1,
    1,
    CURRENT_DATE + INTERVAL '30 days',
    'ACTIVE',
    'Préstamo de prueba base para K6',
    CURRENT_DATE,
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    status = 'ACTIVE',
    agreed_return_date = CURRENT_DATE + INTERVAL '30 days';

-- ─── 6. Insertar movimiento kardex inicial (si no existe) ─────────────
INSERT INTO kardex_movements (id, tool_id, movement_type, quantity, stock_before, stock_after, description, created_at)
VALUES (
    1,
    1,
    'INITIAL_STOCK',
    100,
    0,
    100,
    'Stock inicial - seed para pruebas K6',
    NOW()
)
ON CONFLICT (id) DO NOTHING;

RAISE NOTICE '✅ Datos base insertados correctamente.';

-- ═══════════════════════════════════════════════════════════════════════
-- SEED DE VOLUMEN PARA PRUEBAS DE VOLUMEN
-- Genera ~1.000 préstamos históricos (volumen bajo)
-- ═══════════════════════════════════════════════════════════════════════

DO $$
DECLARE
    i         INTEGER;
    loan_date DATE;
    ret_date  DATE;
BEGIN
    RAISE NOTICE 'Insertando 1.000 préstamos históricos...';

    FOR i IN 2..1001 LOOP
        loan_date := CURRENT_DATE - (random() * 365)::INTEGER;
        ret_date  := loan_date + (random() * 14 + 1)::INTEGER;

        -- Préstamo histórico
        INSERT INTO loans (
            client_id, tool_id, quantity,
            agreed_return_date, actual_return_date,
            status, notes, loan_date, created_at
        )
        VALUES (
            1, 1, 1,
            ret_date, ret_date,
            'RETURNED',
            'Préstamo histórico seed #' || i,
            loan_date,
            loan_date::TIMESTAMP
        )
        ON CONFLICT DO NOTHING;

        -- Movimiento kardex de préstamo
        INSERT INTO kardex_movements (
            tool_id, movement_type, quantity,
            stock_before, stock_after,
            description, created_at
        )
        VALUES (
            1, 'LOAN_OUT', 1,
            100, 99,
            'Movimiento préstamo seed #' || i,
            loan_date::TIMESTAMP
        )
        ON CONFLICT DO NOTHING;

        -- Movimiento kardex de devolución
        INSERT INTO kardex_movements (
            tool_id, movement_type, quantity,
            stock_before, stock_after,
            description, created_at
        )
        VALUES (
            1, 'RETURN', 1,
            99, 100,
            'Movimiento devolución seed #' || i,
            ret_date::TIMESTAMP
        )
        ON CONFLICT DO NOTHING;

    END LOOP;

    RAISE NOTICE '✅ 1.000 préstamos históricos insertados.';
END $$;

-- ═══════════════════════════════════════════════════════════════════════
-- SEED DE VOLUMEN MEDIO: ~10.000 movimientos kardex adicionales
-- (Para la fase "medium" de las pruebas de volumen)
-- ═══════════════════════════════════════════════════════════════════════
DO $$
DECLARE
    i         INTEGER;
    mov_date  TIMESTAMP;
BEGIN
    RAISE NOTICE 'Insertando 10.000 movimientos kardex adicionales...';

    FOR i IN 1..10000 LOOP
        mov_date := NOW() - ((random() * 180)::INTEGER || ' days')::INTERVAL;

        INSERT INTO kardex_movements (
            tool_id, movement_type, quantity,
            stock_before, stock_after,
            description, created_at
        )
        VALUES (
            1,
            CASE (i % 3)
                WHEN 0 THEN 'LOAN_OUT'
                WHEN 1 THEN 'RETURN'
                ELSE        'RESTOCK'
            END,
            1,
            100, 100,
            'Movimiento volumen medio seed #' || i,
            mov_date
        );

    END LOOP;

    RAISE NOTICE '✅ 10.000 movimientos adicionales insertados.';
END $$;

-- ─── Verificación final ───────────────────────────────────────────────
DO $$
DECLARE
    loan_count    INTEGER;
    kardex_count  INTEGER;
    client_count  INTEGER;
    tool_count    INTEGER;
BEGIN
    SELECT COUNT(*) INTO client_count FROM clients;
    SELECT COUNT(*) INTO tool_count   FROM tools;
    SELECT COUNT(*) INTO loan_count   FROM loans;
    SELECT COUNT(*) INTO kardex_count FROM kardex_movements;

    RAISE NOTICE '═══════════════════════════════════════';
    RAISE NOTICE 'RESUMEN DEL SEED:';
    RAISE NOTICE '  Clientes:           %', client_count;
    RAISE NOTICE '  Herramientas:       %', tool_count;
    RAISE NOTICE '  Préstamos:          %', loan_count;
    RAISE NOTICE '  Movimientos Kardex: %', kardex_count;
    RAISE NOTICE '═══════════════════════════════════════';
    RAISE NOTICE 'Seed completado exitosamente. ✅';
    RAISE NOTICE 'Listo para ejecutar pruebas K6.';
END $$;

