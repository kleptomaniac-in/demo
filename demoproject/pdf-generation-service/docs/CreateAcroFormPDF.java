import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.pdmodel.PDResources;

import java.io.IOException;

public class CreateAcroFormPDF {
    public static void main(String[] args) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);

        // Create content on the page
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;
            
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText("Health Insurance Enrollment Form");
            contentStream.endText();
            
            y -= 40;
            contentStream.setFont(PDType1Font.HELVETICA, 11);
            
            String[] labels = {
                "Application Number:", "Effective Date:", "Total Premium:",
                "Primary First Name:", "Primary Last Name:", "Primary DOB:",
                "Primary Gender:", "Primary SSN:", "Street:", "City:", "State:", "Zip:",
                "Spouse First Name:", "Spouse Last Name:", "Spouse DOB:"
            };
            
            for (String label : labels) {
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText(label);
                contentStream.endText();
                y -= 25;
            }
        }

        // Create the AcroForm
        PDAcroForm acroForm = new PDAcroForm(document);
        document.getDocumentCatalog().setAcroForm(acroForm);
        acroForm.setDefaultResources(new PDResources());

        // Add form fields
        float fieldX = 200;
        float fieldY = page.getMediaBox().getHeight() - 90;
        float fieldWidth = 200;
        float fieldHeight = 20;
        
        String[] fieldNames = {
            "ApplicationNumber", "EffectiveDate", "TotalPremium",
            "Primary_FirstName", "Primary_LastName", "Primary_DOB",
            "Primary_Gender", "Primary_SSN", "Primary_Street", "Primary_City",
            "Primary_State", "Primary_Zip", "Spouse_FirstName", "Spouse_LastName", "Spouse_DOB"
        };

        for (String fieldName : fieldNames) {
            PDTextField textField = new PDTextField(acroForm);
            textField.setPartialName(fieldName);
            textField.setDefaultAppearance("/Helv 10 Tf 0 g");
            
            PDRectangle rect = new PDRectangle(fieldX, fieldY, fieldWidth, fieldHeight);
            textField.getWidgets().get(0).setRectangle(rect);
            textField.getWidgets().get(0).setPage(page);
            
            page.getAnnotations().add(textField.getWidgets().get(0));
            acroForm.getFields().add(textField);
            
            fieldY -= 25;
        }

        // Save the PDF
        document.save("src/main/resources/acroforms-temp.pdf");
        document.close();
        
        System.out.println("Created AcroForm PDF");
    }
}
