package com.example.smartwaterchick;

import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Menulis file .xlsx secara manual menggunakan ZipOutputStream bawaan Java.
 * Tidak memerlukan library Apache POI — lebih stabil di Android.
 *
 * Format data sesuai periode:
 *  - Harian  Vol : "Tgl 1"   | "0.153 liter"
 *  - Harian  pH  : "pukul 12.00" | "pH 10,12"
 *  - Mingguan/Bulanan Vol : "Tgl 2"   | "0.153 liter"
 *  - Mingguan/Bulanan pH  : "Tgl 2"   | "pH 6,23"
 *
 * Setiap List berisi pasangan [label, nilai] yang sudah siap tampil.
 * Jadi ukuran list harus genap: index 0=label, 1=nilai, 2=label, 3=nilai, ...
 */
public class XlsxWriter {

    /** Jenis periode laporan */
    public enum Period { DAILY, WEEKLY, MONTHLY }

    private final Period period;
    /** volRows: pasangan (label, nilai) untuk kolom Volume Air */
    private final List<String> volRows;
    /** phRows: pasangan (label, nilai) untuk kolom pH */
    private final List<String> phRows;

    /**
     * @param period  periode laporan (DAILY / WEEKLY / MONTHLY)
     * @param volRows list pasangan label+nilai volume — ukuran harus genap
     * @param phRows  list pasangan label+nilai pH    — ukuran harus genap
     */
    public XlsxWriter(Period period, List<String> volRows, List<String> phRows) {
        this.period  = period;
        this.volRows = volRows;
        this.phRows  = phRows;
    }

    public void write(OutputStream out) throws Exception {
        ZipOutputStream zip = new ZipOutputStream(out);
        putEntry(zip, "[Content_Types].xml",        contentTypes());
        putEntry(zip, "_rels/.rels",                rootRels());
        putEntry(zip, "xl/workbook.xml",            workbook());
        putEntry(zip, "xl/_rels/workbook.xml.rels", workbookRels());
        putEntry(zip, "xl/styles.xml",              styles());
        putEntry(zip, "xl/worksheets/sheet1.xml",   sheet());
        zip.finish();
    }

    // ── ZIP helper ─────────────────────────────────────────────────────────
    private void putEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        byte[] b = content.getBytes("UTF-8");
        zip.write(b, 0, b.length);
        zip.closeEntry();
    }

    // ── Package parts ──────────────────────────────────────────────────────
    private String contentTypes() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\"  ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\""
            +   " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\""
            +   " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\""
            +   " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "</Types>";
    }

    private String rootRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\""
            +   " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\""
            +   " Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";
    }

    private String workbook() {
        String periodName;
        switch (period) {
            case DAILY:   periodName = "Harian";   break;
            case WEEKLY:  periodName = "Mingguan"; break;
            default:      periodName = "Bulanan";  break;
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
            +   " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<sheets><sheet name=\"Laporan " + periodName + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
            + "</workbook>";
    }

    private String workbookRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\""
            +   " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\""
            +   " Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\""
            +   " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\""
            +   " Target=\"styles.xml\"/>"
            + "</Relationships>";
    }

    // ── Styles ─────────────────────────────────────────────────────────────
    // Style index (s="N"):
    //  0 = blank/default
    //  1 = header biru    (bg #1B5BCE, teks putih bold)
    //  2 = header oranye  (bg #F4A435, teks putih bold)
    //  3 = sub-header biru(bg #BDD7FF, teks biru bold, border)
    //  4 = sub-header orng(bg #FFD9A0, teks orng bold, border)
    //  5 = data           (center, border)
    private String styles() {
        String fonts =
            "<fonts count=\"5\">"
            + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"10\"/><color rgb=\"FF1B5BCE\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"10\"/><color rgb=\"FFF4A435\"/><name val=\"Calibri\"/></font>"
            + "</fonts>";

        String fills =
            "<fills count=\"6\">"
            + "<fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1B5BCE\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF4A435\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFBDD7FF\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFFD9A0\"/></patternFill></fill>"
            + "</fills>";

        String thinBorder = "<left style=\"thin\"><color rgb=\"FF808080\"/></left>"
            + "<right style=\"thin\"><color rgb=\"FF808080\"/></right>"
            + "<top style=\"thin\"><color rgb=\"FF808080\"/></top>"
            + "<bottom style=\"thin\"><color rgb=\"FF808080\"/></bottom>"
            + "<diagonal/>";
        String borders =
            "<borders count=\"2\">"
            + "<border><left/><right/><top/><bottom/><diagonal/></border>"
            + "<border>" + thinBorder + "</border>"
            + "</borders>";

        String csxfs = "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>";

        String cxfs =
            "<cellXfs count=\"6\">"
            // 0: default
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            // 1: header biru
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\">"
            +   "<alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            // 2: header oranye
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"3\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\">"
            +   "<alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            // 3: sub biru
            + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"4\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">"
            +   "<alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            // 4: sub oranye
            + "<xf numFmtId=\"0\" fontId=\"4\" fillId=\"5\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">"
            +   "<alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            // 5: data
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\">"
            +   "<alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            + "</cellXfs>";

        String cellStyles = "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>";

        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + fonts + fills + borders + csxfs + cxfs + cellStyles
            + "</styleSheet>";
    }

    // ── Worksheet ──────────────────────────────────────────────────────────
    private String sheet() {
        // Hitung jumlah baris data (tiap entry = 1 baris, karena label & nilai di kolom berbeda)
        int volCount = volRows.size() / 2;  // tiap entry = 2 elemen (label, nilai)
        int phCount  = phRows.size()  / 2;
        int maxRows  = Math.max(volCount, phCount);
        if (maxRows == 0) maxRows = 1;

        String periodName;
        switch (period) {
            case DAILY:   periodName = "Harian";   break;
            case WEEKLY:  periodName = "Mingguan"; break;
            default:      periodName = "Bulanan";  break;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");

        // Lebar kolom: A=label vol, B=nilai vol, C=spacer, D=label pH, E=nilai pH
        sb.append("<cols>");
        sb.append("<col min=\"1\" max=\"2\" width=\"20\" customWidth=\"1\"/>");
        sb.append("<col min=\"3\" max=\"3\" width=\"4\"  customWidth=\"1\"/>");
        sb.append("<col min=\"4\" max=\"5\" width=\"20\" customWidth=\"1\"/>");
        sb.append("</cols>");

        sb.append("<sheetData>");

        // Baris 1 — judul
        sb.append("<row r=\"1\" ht=\"25\" customHeight=\"1\">");
        sb.append(cell("A1", "Laporan Penggunaan Air (" + periodName + ")", 1));
        sb.append(cell("B1", "", 1));
        sb.append(cell("C1", "", 0));
        sb.append(cell("D1", "Riwayat Kondisi pH (" + periodName + ")", 2));
        sb.append(cell("E1", "", 2));
        sb.append("</row>");

        // Baris 2 — sub-header
        String volSubLabel = (period == Period.DAILY) ? "Waktu" : "Tanggal";
        String phSubLabel  = (period == Period.DAILY) ? "Jam"   : "Tanggal";
        sb.append("<row r=\"2\" ht=\"20\" customHeight=\"1\">");
        sb.append(cell("A2", volSubLabel,    3));
        sb.append(cell("B2", "Volume Air",  3));
        sb.append(cell("C2", "", 0));
        sb.append(cell("D2", phSubLabel,     4));
        sb.append(cell("E2", "Nilai pH",    4));
        sb.append("</row>");

        // Baris data mulai dari baris 3
        for (int i = 0; i < maxRows; i++) {
            int r = i + 3;
            // label & nilai volume
            String volLabel = getPair(volRows, i, 0);
            String volVal   = getPair(volRows, i, 1);
            // label & nilai pH
            String phLabel  = getPair(phRows,  i, 0);
            String phVal    = getPair(phRows,  i, 1);

            sb.append("<row r=\"").append(r).append("\" ht=\"18\" customHeight=\"1\">");
            sb.append(cell("A" + r, volLabel, 5));
            sb.append(cell("B" + r, volVal,   5));
            sb.append(cell("C" + r, "",        0));
            sb.append(cell("D" + r, phLabel,  5));
            sb.append(cell("E" + r, phVal,    5));
            sb.append("</row>");
        }

        sb.append("</sheetData>");

        // mergeCells HARUS setelah sheetData (per OOXML spec)
        sb.append("<mergeCells count=\"2\">");
        sb.append("<mergeCell ref=\"A1:B1\"/>");
        sb.append("<mergeCell ref=\"D1:E1\"/>");
        sb.append("</mergeCells>");

        sb.append("</worksheet>");
        return sb.toString();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Ambil elemen ke-i dari list pasangan.
     * @param list  list berisi [label0, val0, label1, val1, ...]
     * @param entry indeks entry (0-based)
     * @param col   0=label, 1=nilai
     */
    private String getPair(List<String> list, int entry, int col) {
        int idx = entry * 2 + col;
        if (idx < list.size() && list.get(idx) != null) return list.get(idx);
        return "-";
    }

    /** Inline string cell dengan style index */
    private String cell(String ref, String value, int s) {
        return "<c r=\"" + ref + "\" t=\"inlineStr\" s=\"" + s + "\">"
             + "<is><t>" + esc(value) + "</t></is></c>";
    }

    private String esc(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&apos;");
    }
}
