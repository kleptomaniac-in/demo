import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import java.io.*;

public class ListAcroFormFields {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java ListAcroFormFields <pdf-file>");
            return;
        }
        
        PDDocument doc = PDDocument.load(new File(args[0]));
        PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
        
        if (form != null) {
            System.out.println("Fields in PDF:");
            for (PDField field : form.getFields()) {
                System.out.println("  - " + field.getFullyQualifiedName());
            }
        } else {
            System.out.println("No AcroForm found in PDF");
        }
        doc.close();
    }
}
