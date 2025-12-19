import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import java.io.*;

public class ReadAcroFormValues {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java ReadAcroFormValues <pdf-file>");
            return;
        }
        
        PDDocument doc = PDDocument.load(new File(args[0]));
        PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
        
        if (form != null) {
            System.out.println("Field Values in PDF:");
            System.out.println("===================");
            for (PDField field : form.getFields()) {
                String name = field.getFullyQualifiedName();
                String value = field.getValueAsString();
                System.out.println(String.format("%-30s = %s", name, value != null ? value : "(empty)"));
            }
        } else {
            System.out.println("No AcroForm found in PDF");
        }
        doc.close();
    }
}
