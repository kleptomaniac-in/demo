import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.File;

public class AddFormFields {
    public static void main(String[] args) throws Exception {
        // Load existing PDF
        PDDocument document = PDDocument.load(new File("src/main/resources/templates/enrollment-form-base.pdf"));
        
        // Create AcroForm if it doesn't exist
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
        }
        
        // Add text fields (invisible, just data carriers)
        String[] fieldNames = {
            "ApplicationNumber", "EffectiveDate", "TotalPremium",
            "Primary_FirstName", "Primary_LastName", "Primary_DOB", 
            "Primary_Gender", "Primary_SSN",
            "Primary_Street", "Primary_City", "Primary_State", "Primary_Zip",
            "Spouse_FirstName", "Spouse_LastName", "Spouse_DOB"
        };
        
        for (String fieldName : fieldNames) {
            PDTextField textField = new PDTextField(acroForm);
            textField.setPartialName(fieldName);
            acroForm.getFields().add(textField);
        }
        
        // Save to new file
        document.save("src/main/resources/templates/enrollment-form.pdf");
        document.close();
        
        System.out.println("Added form fields to enrollment-form.pdf");
    }
}
