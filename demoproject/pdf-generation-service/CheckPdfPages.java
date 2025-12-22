
import org.apache.pdfbox.pdmodel.PDDocument;
import java.io.File;
import java.io.IOException;

public class CheckPdfPages {
    public static void main(String[] args) {
        try {
            File file = new File("src/main/resources/templates/enrollment-form-base.pdf");
            if (file.exists()) {
                PDDocument doc = PDDocument.load(file);
                System.out.println("Pages: " + doc.getNumberOfPages());
                doc.close();
            } else {
                System.out.println("File not found: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
