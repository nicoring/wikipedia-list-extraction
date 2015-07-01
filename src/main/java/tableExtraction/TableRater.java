package tableExtraction;

public class TableRater {

    private final static int UNIQUE_FACTOR = 10;
    private final static int LEFT_FACTOR = 10;
    private final static int COLUMN_MATCH_FACTOR = 3;

    /*
    public int[] rateColumnMatches(RDFTable table) {
        int columnCount = table.getColumnCount();
        int[] matchedColumns = new int[columnCount];
        PredicateMatcher matcher = new PredicateMatcher();

        for (int i = 0; i < columnCount; i++) {
            matchedColumns[i] = matcher.countMatchingColumns(i, table) * COLUMN_MATCH_FACTOR;
        }

        return matchedColumns;
    }

    public int[] rateLeftness(RDFTable table) {
        int columnCount = table.getColumnCount();
        int[] leftValues = new int[columnCount];

        for (int i = 0; i < columnCount; i++) {
            leftValues[i] = (int) Math.floor(LEFT_FACTOR - i * (LEFT_FACTOR / columnCount));
        }

        return leftValues;
    }

    public int[] getUniquenessValues(RDFTable table) {
        boolean[] unique = testColumnsUnique(table);
        int[] uniquenessValues = new int[unique.length];
        for (int i = 0; i < unique.length; i++) {
            if (unique[i]) {
                uniquenessValues[i] = UNIQUE_FACTOR;
            }
        }
        return uniquenessValues;
    }

    private boolean[] testColumnsUnique(RDFTable table) {
        int columnCount = table.getColumnCount();
        boolean[] results = new boolean[columnCount];
        for (int i = 0; i < columnCount; i++) {
            List<String> rdfTitles = table.getColumnAsRawString(i);
            results[i] = isColumnUnique(rdfTitles);
        }

        return results;
    }

    private boolean isColumnUnique(List<String> column) {
        Set<String> columnWithoutDuplicates = new HashSet<>(column);
        return (column.size() == columnWithoutDuplicates.size());
    }

    private int findMaxColumn(int[] rating) {
        int max = 0;
        int maxPosition = 0;
        for (int i = 0; i < rating.length; i++) {
            if (rating[i] > max) {
                max = rating[i];
                maxPosition = i;
            }
        }
        return maxPosition;
    }

    private int[] rateColumnRelationships() {
        return null;
    }
    */
}