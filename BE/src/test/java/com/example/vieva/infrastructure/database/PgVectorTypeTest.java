package com.example.vieva.infrastructure.database;

import com.pgvector.PGvector;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PgVectorTypeTest {

    private final PgVectorType type = new PgVectorType();

    @Test
    void nullSafeSetWritesPgvectorWithVectorType() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);

        type.nullSafeSet(ps, new float[]{0.5f, -1f, 2f}, 1, null);

        org.mockito.ArgumentCaptor<Object> cap = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(ps).setObject(eq(1), cap.capture());
        Object written = cap.getValue();
        assertTrue(written instanceof PGvector, "expected PGvector but got " + written.getClass());
        assertEquals("vector", ((PGvector) written).getType());
        assertEquals("[0.5,-1.0,2.0]", ((PGvector) written).getValue());
    }

    @Test
    void nullSafeSetWritesNullAsOther() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);

        type.nullSafeSet(ps, null, 3, null);

        verify(ps).setNull(eq(3), eq(java.sql.Types.OTHER));
    }

    @Test
    void nullSafeGetParsesVectorLiteralText() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(anyInt())).thenReturn("[1.5, 2, -3.25]");

        assertArrayEquals(new float[]{1.5f, 2f, -3.25f}, type.nullSafeGet(rs, 1, null, null));
    }

    @Test
    void nullSafeGetUnwrapsRegisteredPgvector() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(anyInt())).thenReturn(new PGvector(new float[]{7f, 8f}));

        assertArrayEquals(new float[]{7f, 8f}, type.nullSafeGet(rs, 1, null, null));
    }

    @Test
    void nullSafeGetReturnsNull() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(anyInt())).thenReturn(null);

        assertNull(type.nullSafeGet(rs, 1, null, null));
    }

    @Test
    void deepCopyIsDefensive() {
        float[] original = {1f, 2f};
        float[] copy = type.deepCopy(original);
        assertArrayEquals(original, copy);
        copy[0] = 9f;
        assertEquals(1f, original[0]);
    }
}
