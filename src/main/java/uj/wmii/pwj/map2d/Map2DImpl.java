package uj.wmii.pwj.map2d;

import java.util.*;
import java.util.function.Function;

public final class Map2DImpl<R, C, V> implements Map2D<R, C, V> {

    private final Map<R, Map<C, V>> data;

    public Map2DImpl() {
        this.data = new HashMap<>();
    }

    private Map2DImpl(Map<R, Map<C, V>> data) {
        this.data = new HashMap<>();
        for (Map.Entry<R, Map<C, V>> rowEntry : data.entrySet()) {
            Map<C, V> rowCopy = new HashMap<>(rowEntry.getValue());
            this.data.put(rowEntry.getKey(), rowCopy);
        }
    }

    @Override
    public V put(R rowKey, C columnKey, V value) {
        Objects.requireNonNull(rowKey, "rowKey cannot be null");
        Objects.requireNonNull(columnKey, "columnKey cannot be null");

        Map<C, V> row = data.computeIfAbsent(rowKey, k -> new HashMap<>());
        return row.put(columnKey, value);
    }

    @Override
    public V get(R rowKey, C columnKey) {
        Map<C, V> row = data.get(rowKey);
        if (row == null) {
            return null;
        }
        return row.get(columnKey);
    }

    @Override
    public V getOrDefault(R rowKey, C columnKey, V defaultValue) {
        V value = get(rowKey, columnKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public V remove(R rowKey, C columnKey) {
        Map<C, V> row = data.get(rowKey);
        if (row == null) {
            return null;
        }
        V removed = row.remove(columnKey);
        if (row.isEmpty()) {
            data.remove(rowKey);
        }
        return removed;
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean nonEmpty() {
        return !isEmpty();
    }

    @Override
    public int size() {
        int count = 0;
        for (Map<C, V> row : data.values()) {
            count += row.size();
        }
        return count;
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public Map<C, V> rowView(R rowKey) {
        Map<C, V> row = data.get(rowKey);
        if (row == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(row));
    }

    @Override
    public Map<R, V> columnView(C columnKey) {
        Map<R, V> column = new HashMap<>();
        for (Map.Entry<R, Map<C, V>> rowEntry : data.entrySet()) {
            V value = rowEntry.getValue().get(columnKey);
            if (value != null) {
                column.put(rowEntry.getKey(), value);
            }
        }
        return Collections.unmodifiableMap(column);
    }

    @Override
    public boolean containsValue(V value) {
        for (Map<C, V> row : data.values()) {
            if (row.containsValue(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsKey(R rowKey, C columnKey) {
        Map<C, V> row = data.get(rowKey);
        return row != null && row.containsKey(columnKey);
    }

    @Override
    public boolean containsRow(R rowKey) {
        return data.containsKey(rowKey) && !data.get(rowKey).isEmpty();
    }

    @Override
    public boolean containsColumn(C columnKey) {
        for (Map<C, V> row : data.values()) {
            if (row.containsKey(columnKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map<R, Map<C, V>> rowMapView() {
        Map<R, Map<C, V>> result = new HashMap<>();
        for (Map.Entry<R, Map<C, V>> rowEntry : data.entrySet()) {
            result.put(rowEntry.getKey(), Collections.unmodifiableMap(new HashMap<>(rowEntry.getValue())));
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public Map<C, Map<R, V>> columnMapView() {
        Map<C, Map<R, V>> result = new HashMap<>();

        for (Map.Entry<R, Map<C, V>> rowEntry : data.entrySet()) {
            R rowKey = rowEntry.getKey();
            for (Map.Entry<C, V> cellEntry : rowEntry.getValue().entrySet()) {
                C columnKey = cellEntry.getKey();
                result.computeIfAbsent(columnKey, k -> new HashMap<>())
                        .put(rowKey, cellEntry.getValue());
            }
        }

        Map<C, Map<R, V>> unmodifiableResult = new HashMap<>();
        for (Map.Entry<C, Map<R, V>> entry : result.entrySet()) {
            unmodifiableResult.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
        }
        return Collections.unmodifiableMap(unmodifiableResult);
    }

    @Override
    public Map2D<R, C, V> fillMapFromRow(Map<? super C, ? super V> target, R rowKey) {
        Map<C, V> row = data.get(rowKey);
        if (row != null) {
            target.putAll(row);
        }
        return this;
    }

    @Override
    public Map2D<R, C, V> fillMapFromColumn(Map<? super R, ? super V> target, C columnKey) {
        for (Map.Entry<R, Map<C, V>> rowEntry : data.entrySet()) {
            V value = rowEntry.getValue().get(columnKey);
            if (value != null) {
                target.put(rowEntry.getKey(), value);
            }
        }
        return this;
    }

    @Override
    public Map2D<R, C, V> putAll(Map2D<? extends R, ? extends C, ? extends V> source) {
        if (source instanceof Map2DImpl) {
            Map2DImpl<? extends R, ? extends C, ? extends V> sourceImpl =
                    (Map2DImpl<? extends R, ? extends C, ? extends V>) source;
            for (Map.Entry<? extends R, ? extends Map<? extends C, ? extends V>> rowEntry :
                    sourceImpl.data.entrySet()) {
                Map<C, V> targetRow = data.computeIfAbsent(rowEntry.getKey(), k -> new HashMap<>());
                for (Map.Entry<? extends C, ? extends V> cellEntry : rowEntry.getValue().entrySet()) {
                    targetRow.put(cellEntry.getKey(), cellEntry.getValue());
                }
            }
        } else {
            // Fallback for other implementations
            Map<? extends R, ? extends Map<? extends C, ? extends V>> rowMap = source.rowMapView();
            for (Map.Entry<? extends R, ? extends Map<? extends C, ? extends V>> rowEntry : rowMap.entrySet()) {
                Map<C, V> targetRow = data.computeIfAbsent(rowEntry.getKey(), k -> new HashMap<>());
                for (Map.Entry<? extends C, ? extends V> cellEntry : rowEntry.getValue().entrySet()) {
                    targetRow.put(cellEntry.getKey(), cellEntry.getValue());
                }
            }
        }
        return this;
    }

    @Override
    public Map2D<R, C, V> putAllToRow(Map<? extends C, ? extends V> source, R rowKey) {
        Map<C, V> row = data.computeIfAbsent(rowKey, k -> new HashMap<>());
        row.putAll(source);
        return this;
    }

    @Override
    public Map2D<R, C, V> putAllToColumn(Map<? extends R, ? extends V> source, C columnKey) {
        for (Map.Entry<? extends R, ? extends V> entry : source.entrySet()) {
            put(entry.getKey(), columnKey, entry.getValue());
        }
        return this;
    }

    @Override
    public <R2, C2, V2> Map2D<R2, C2, V2> copyWithConversion(
            Function<? super R, ? extends R2> rowFunction,
            Function<? super C, ? extends C2> columnFunction,
            Function<? super V, ? extends V2> valueFunction) {

        Map<R2, Map<C2, V2>> resultData = new HashMap<>();

        for (Map.Entry<R, Map<C, V>> rowEntry : data.entrySet()) {
            R2 newRowKey = rowFunction.apply(rowEntry.getKey());
            for (Map.Entry<C, V> cellEntry : rowEntry.getValue().entrySet()) {
                C2 newColumnKey = columnFunction.apply(cellEntry.getKey());
                V2 newValue = valueFunction.apply(cellEntry.getValue());

                resultData.computeIfAbsent(newRowKey, k -> new HashMap<>())
                        .put(newColumnKey, newValue);
            }
        }

        return new Map2DImpl<>(resultData);
    }

    public static <R, C, V> Map2D<R, C, V> createInstance() {
        return new Map2DImpl<>();
    }
}