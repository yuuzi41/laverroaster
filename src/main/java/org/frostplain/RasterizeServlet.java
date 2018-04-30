package org.frostplain;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;

@MultipartConfig(location = "/tmp", maxFileSize = -1L)
public class RasterizeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        resp.setContentType("text/html");

        PrintWriter writer = resp.getWriter();
        writer.println("<html><body>");
        writer.println("<form method=\"post\" action=\"./\" enctype=\"multipart/form-data\">");
        writer.println("<p><input type=\"file\" name=\"file\" size=\"30\"></p>\n");
        writer.println("<p><input type=\"submit\" value=\"submit\"></p>\n");
        writer.println("</form>");
        writer.println("</body></html>");
        writer.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        Part part = req.getPart("file");

        log(String.format("Request: filename:%s", part.getSubmittedFileName()));

        try (PDDocument srcdoc = PDDocument.load(part.getInputStream());
             PDDocument dstdoc = new PDDocument()) {
            PDFRenderer renderer = new PDFRenderer(srcdoc);

            for (int i = 0; i < srcdoc.getNumberOfPages(); i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, 300f, ImageType.RGB);

                float imgWidth = img.getWidth(), imgHeight = img.getHeight();
                float imgScale = 72f / 300f;

                PDPage dstpage = new PDPage(new PDRectangle(imgWidth * imgScale, imgHeight * imgScale));
                dstdoc.addPage(dstpage);
                log(String.format(
                        "PageProcess: filename:%s page:%d mediabox:%s",
                        part.getSubmittedFileName(), i, dstpage.getMediaBox().toString()
                        )
                );

                try (PDPageContentStream dstpageStream = new PDPageContentStream(dstdoc, dstpage)) {
                    PDImageXObject image = JPEGFactory.createFromImage(dstdoc, img);
                    dstpageStream.drawImage(image, 0, 0, imgWidth * imgScale, imgHeight * imgScale);
                }
            }
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition", "attachment; filename=\"out.pdf\"");
            dstdoc.save(resp.getOutputStream());
        }
    }
}
