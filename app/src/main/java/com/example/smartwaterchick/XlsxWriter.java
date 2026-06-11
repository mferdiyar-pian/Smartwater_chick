package com.example.smartwaterchick;

import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Menulis file .xlsx secara manual menggunakan ZipOutputStream bawaan Java.
 * Tidak memerlukan library Apache POI — lebih stabil di Android.
 */
public class XlsxWriter {

    private final List<String> dailyVol, weeklyVol, monthlyVol;
    private final List<String> dailyPh,  weeklyPh,  monthlyPh;

    public XlsxWriter(List<String> dailyVol,  List<String> weeklyVol,  List<String> monthlyVol,
                      List<String> dailyPh,   List<String> weeklyPh,   List<String> monthlyPh) {
        this.dailyVol   = dailyVol;
        this.weeklyVol  = weeklyVol;
        this.monthlyVol = monthlyVol;
        this.dailyPh    = dailyPh;
        this.weeklyPh   = weeklyPh;
        this.monthlyPh  = monthlyPh;
    }

    public void write(OutputStream out) throws Exception {
        ZipOutputStream zip = new ZipOutputStream(out);
        putEntry(zip, "[Content_Types].xml",       contentTypes());
        putEntry(zip, "_rels/.rels",               rootRels());
        putEntry(zip, "xl/workbook.xml",           workbook());
        putEntry(zip, "xl/_rels/workbook.xml.rels",workbookRels());
        putEntry(zip, "xl/styles.xml",             styles());
        putEntry(zip, "xl/worksheets/sheet1.xml",  sheet());
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
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
            +   " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<sheets><sheet name=\"Laporan\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
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
        int maxRows = Math.max(
            Math.max(dailyVol.size(), Math.max(weeklyVol.size(), monthlyVol.size())),
            Math.max(dailyPh.size(),  Math.max(weeklyPh.size(),  monthlyPh.size()))
        );
        if (maxRows == 0) maxRows = 1;

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");

        // Lebar kolom
        sb.append("<cols>");
        sb.append("<col min=\"1\" max=\"3\" width=\"18\" customWidth=\"1\"/>");
        sb.append("<col min=\"4\" max=\"4\" width=\"3\"  customWidth=\"1\"/>");
        sb.append("<col min=\"5\" max=\"7\" width=\"18\" customWidth=\"1\"/>");
        sb.append("</cols>");

        // Data
        sb.append("<sheetData>");

        // Baris 1 — judul
        sb.append("<row r=\"1\" ht=\"25\" customHeight=\"1\">");
        sb.append(cell("A1", "Laporan Penggunaan Air", 1));
        sb.append(cell("B1", "", 1));
        sb.append(cell("C1", "", 1));
        sb.append(cell("D1", "", 0));
        sb.append(cell("E1", "Riwayat Kondisi pH", 2));
        sb.append(cell("F1", "", 2));
        sb.append(cell("G1", "", 2));
        sb.append("</row>");

        // Baris 2 — sub-header
        sb.append("<row r=\"2\" ht=\"20\" customHeight=\"1\">");
        sb.append(cell("A2", "Harian",   3));
        sb.append(cell("B2", "Mingguan", 3));
        sb.append(cell("C2", "Bulanan",  3));
        sb.append(cell("D2", "", 0));
        sb.append(cell("E2", "Harian",   4));
        sb.append(cell("F2", "Mingguan", 4));
        sb.append(cell("G2", "Bulanan",  4));
        sb.append("</row>");

        // Baris data mulai dari baris 3
        String[] cols = {"A","B","C","D","E","F","G"};
        for (int i = 0; i < maxRows; i++) {
            int r = i + 3;
            sb.append("<row r=\"").append(r).append("\" ht=\"18\" customHeight=\"1\">");
            sb.append(cell(cols[0]+r, get(dailyVol,   i), 5));
            sb.append(cell(cols[1]+r, get(weeklyVol,  i), 5));
            sb.append(cell(cols[2]+r, get(monthlyVol, i), 5));
            sb.append(cell(cols[3]+r, "",               0));
            sb.append(cell(cols[4]+r, get(dailyPh,    i), 5));
            sb.append(cell(cols[5]+r, get(weeklyPh,   i), 5));
            sb.append(cell(cols[6]+r, get(monthlyPh,  i), 5));
            sb.append("</row>");
        }

        sb.append("</sheetData>");

        // mergeCells HARUS setelah sheetData (per OOXML spec)
        sb.append("<mergeCells count=\"2\">");
        sb.append("<mergeCell ref=\"A1:C1\"/>");
        sb.append("<mergeCell ref=\"E1:G1\"/>");
        sb.append("</mergeCells>");

        sb.append("</worksheet>");
        return sb.toString();
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private String get(List<String> list, int i) {
        return (i < list.size() && list.get(i) != null) ? list.get(i) : "-";
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
