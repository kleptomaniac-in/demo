import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateAcroFormTemplate {
    public static void main(String[] args) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);
        
        // Create the form
        PDAcroForm acroForm = new PDAcroForm(document);
        document.getDocumentCatalog().setAcroForm(acroForm);
        
        // Set up appearance characteristics
        acroForm.setNeedAppearances(true);
        
        // Create content stream for labels
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
        contentStream.beginText();
        contentStream.newLineAtOffset(50, 750);
        contentStream.showText("Enrollment Application Form");
        contentStream.endText();
        
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        
        // Application Information Section
        float y = 710;
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
        contentStream.newLineAtOffset(50, y);
        contentStream.showText("Application Information");
        contentStream.endText();
        y -= 25;
        
        // Add fields with labels
        addFieldLabel(contentStream, 50, y, "Application Number:");
        addTextField(acroForm, page, "ApplicationNumber", 180, y - 5, 150, 20);
        
        y -= 25;
        addFieldLabel(contentStream, 50, y, "Effective Date:");
        addTextField(acroForm, page, "EffectiveDate", 180, y - 5, 150, 20);
        
        y -= 25;
        addFieldLabel(contentStream, 50, y, "Total Premium:");
        addTextField(acroForm, page, "TotalPremium", 180, y - 5, 150, 20);
        
        // Primary Applicant Section
        y -= 40;
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
        contentStream.newLineAtOffset(50, y);
        contentStream.showText("Primary Applicant");
        contentStream.endText();
        y -= 25;
        
        addFieldLabel(contentStream, 50, y, "First Name:");
        addTextField(acroForm, page, "Primary_FirstName", 130, y - 5, 100, 20);
        
        addFieldLabel(contentStream, 250, y, "Last Name:");
        addTextField(acroForm, page, "Primary_LastName", 320, y - 5, 100, 20);
        
        y -= 25;
        addFieldLabel(contentStream, 50, y, "Date of Birth:");
        addTextField(acroForm, page, "Primary_DOB", 130, y - 5, 100, 20);
        
        addFieldLabel(contentStream, 250, y, "Gender:");
        addTextField(acroForm, page, "Primary_Gender", 320, y - 5, 100, 20);
        
        y -= 25;
        addFieldLabel(contentStream, 50, y, "SSN:");
        addTextField(acroForm, page, "Primary_SSN", 130, y - 5, 100, 20);
        
        // Address Section
        y -= 40;
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
        contentStream.newLineAtOffset(50, y);
        contentStream.showText("Mailing Address");
        contentStream.endText();
        y -= 25;
        
        addFieldLabel(contentStream, 50, y, "Street:");
        addTextField(acroForm, page, "Primary_Street", 130, y - 5, 290, 20);
        
        y -= 25;
        addFieldLabel(contentStream, 50, y, "City:");
        addTextField(acroForm, page, "Primary_City", 130, y - 5, 100, 20);
        
        addFieldLabel(contentStream, 250, y, "State:");
        addTextField(acroForm, page, "Primary_State", 290, y - 5, 40, 20);
        
        addFieldLabel(contentStream, 350, y, "Zip:");
        addTextField(acroForm, page, "Primary_Zip", 380, y - 5, 60, 20);
        
        // Spouse Section
        y -= 40;
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
        contentStream.newLineAtOffset(50, y);
        contentStream.showText("Spouse (if applicable)");
        contentStream.endText();
        y -= 25;
        
        addFieldLabel(contentStream, 50, y, "First Name:");
        addTextField(acroForm, page, "Spouse_FirstName", 130, y - 5, 100, 20);
        
        addFieldLabel(contentStream, 250, y, "Last Name:");
        addTextField(acroForm, page, "Spouse_LastName", 320, y - 5, 100, 20);
        
        y -= 25;
        addFieldLabel(contentStream, 50, y, "Date of Birth:");
        addTextField(acroForm, page, "Spouse_DOB", 130, y - 5, 100, 20);
        
        // Dependents Section
        y -= 40;
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
        contentStream.newLineAtOffset(50, y);
        contentStream.showText("Dependents");
        contentStream.endText();
        y -= 25;
        
        // Dependent 1
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.newLineAtOffset(50, y);
        contentStream.showText("Dependent 1:");
        contentStream.endText();
        y -= 20;
        
        addFieldLabel(contentStream, 60, y, "First:");
        addTextField(acroForm, page, "Dependent1_FirstName", 100, y - 5, 70, 18);
        
        addFieldLabel(contentStream, 180, y, "Last:");
        addTextField(acroForm, page, "Dependent1_LastName", 210, y - 5, 70, 18);
        
        addFieldLabel(contentStream, 290, y, "DOB:");
        addTextField(acroForm, page, "Dependent1_DOB", 320, y - 5, 70, 18);
        
        addFieldLabel(contentStream, 400, y, "Gender:");
        addTextField(acroForm, page, "Dependent1_Gender", 445, y - 5, 50, 18);
        
        // Dependent 2
        y -= 25;
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.newLineAtOffset(50, y);
        contentStream.showText("Dependent 2:");
        contentStream.endText();
        y -= 20;
        
        addFieldLabel(contentStream, 60, y, "First:");
        addTextField(acroForm, page, "Dependent2_FirstName", 100, y - 5, 70, 18);
        
        addFieldLabel(contentStream, 180, y, "Last:");
        addTextField(acroForm, page, "Dependent2_LastName", 210, y - 5, 70, 18);
        
        addFieldLabel(contentStream, 290, y, "DOB:");
        addTextField(acroForm, page, "Dependent2_DOB", 320, y - 5, 70, 18);
        
        addFieldLabel(contentStream, 400, y, "Gender:");
        addTextField(acroForm, page, "Dependent2_Gender", 445, y - 5, 50, 18);
        
        contentStream.close();
        
        // Save the document
        document.save("src/main/resources/templates/enrollment-form-base.pdf");
        document.close();
        
        System.out.println("AcroForm template created: src/main/resources/templates/enrollment-form-base.pdf");
    }
    
    private static void addFieldLabel(PDPageContentStream contentStream, float x, float y, String label) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(label);
        contentStream.endText();
    }
    
    private static void addTextField(PDAcroForm acroForm, PDPage page, String fieldName, 
                                     float x, float y, float width, float height) throws IOException {
        PDTextField textField = new PDTextField(acroForm);
        textField.setPartialName(fieldName);
        
        PDAnnotationWidget widget = new PDAnnotationWidget();
        PDRectangle rect = new PDRectangle(x, y, width, height);
        widget.setRectangle(rect);
        widget.setPage(page);
        
        textField.setWidgets(java.util.Collections.singletonList(widget));
        
        acroForm.getFields().add(textField);
        page.getAnnotations().add(widget);
    }
}
