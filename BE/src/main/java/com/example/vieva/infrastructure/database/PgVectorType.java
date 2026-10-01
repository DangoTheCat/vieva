package com.example.vieva.infrastructure.database;

import com.pgvector.PGvector;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

/**
 * Hibernate {@link UserType} mapping a {@code float[]} Java field to a PostgreSQL
 * {@code vector} column (pgvector extension).
 *
 * <p>Hibernate 6.6 knows {@code SqlTypes.VECTOR} but that requires the optional
 * {@code hibernate-vector} module. This project already depends on
 * {@code com.pgvector:pgvector} (pulled in by the Spring AI PGVector store), so a
 * small self-contained UserType is used instead.
 *
 * <p>{@link #getSqlType()} returns {@link Types#OTHER}, matching what the
 * PostgreSQL JDBC driver reports for the {@code vector} type, so
 * {@code ddl-auto=validate} accepts the column.
 */
public class PgVectorType implements UserType<float[]> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<float[]> returnedClass() {
        return float[].class;
    }

    @Override
    public boolean equals(float[] x, float[] y) {
        return Arrays.equals(x, y);
    }

    @Override
    public int hashCode(float[] x) {
        return Arrays.hashCode(x);
    }

    @Override
    public float[] nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        Object value = rs.getObject(position);
        if (value == null) {
            return null;
        }
        if (value instanceof PGvector vector) {
            return vector.toArray();
        }
        return parse(value.toString());
    }

    @Override
    public void nullSafeSet(PreparedStatement st, float[] value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
            return;
        }
        st.setObject(index, new PGvector(value));
    }

    @Override
    public float[] deepCopy(float[] value) {
        return value == null ? null : value.clone();
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(float[] value) {
        return deepCopy(value);
    }

    @Override
    public float[] assemble(Serializable cached, Object owner) {
        return cached == null ? null : deepCopy((float[]) cached);
    }

    private static float[] parse(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("]")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            return new float[0];
        }
        String[] parts = trimmed.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
