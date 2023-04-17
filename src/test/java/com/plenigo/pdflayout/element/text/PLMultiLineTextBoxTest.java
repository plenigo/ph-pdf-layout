/*
 * Copyright (C) 2014-2023 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.plenigo.pdflayout.element.text;

import com.plenigo.pdflayout.PDFCreationException;
import com.plenigo.pdflayout.PLDebugTestRule;
import com.plenigo.pdflayout.PageLayoutPDF;
import com.plenigo.pdflayout.base.PLPageSet;
import com.plenigo.pdflayout.element.hbox.PLHBox;
import com.plenigo.pdflayout.element.special.PLSpacerY;
import com.plenigo.pdflayout.spec.FontSpec;
import com.plenigo.pdflayout.spec.PreloadFont;
import com.plenigo.pdflayout.spec.WidthSpec;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.io.File;

/**
 * Test class for {@link PLMultiLineTextBox}
 */
public final class PLMultiLineTextBoxTest {
    @Rule
    public final TestRule m_aRule = new PLDebugTestRule();

    @Test
    public void testMultiTextLines() throws PDFCreationException {
        final FontSpec r10 = new FontSpec(PreloadFont.REGULAR, 10);
        final FontSpec r10b = new FontSpec(PreloadFont.REGULAR_BOLD, 10);
        final FontSpec r6 = new FontSpec(PreloadFont.REGULAR, 6);
        final FontSpec r20 = new FontSpec(PreloadFont.REGULAR, 20);
        final FontSpec r20b = new FontSpec(PreloadFont.REGULAR_BOLD, 20);
        final FontSpec r14 = new FontSpec(PreloadFont.REGULAR, 14);

        final PLPageSet aPS1 = new PLPageSet(PDRectangle.A4);

        aPS1.addElement(getPLMultiLineTextBox());

        aPS1.addElement(new PLSpacerY(20f));

        aPS1.addElement(getPLText());

        aPS1.addElement(new PLSpacerY(20f));

        PLHBox aHBox = new PLHBox();
        aHBox.addColumn(getPLMultiLineTextBox(), WidthSpec.perc(50));

        aHBox.addColumn(getPLText(), WidthSpec.perc(50));

        aPS1.addElement(aHBox);

        aPS1.addElement(new PLSpacerY(20f));

        final PLMultiLineTextBox aMLBox = new PLMultiLineTextBox();
        aMLBox.addMultiLineText(new PLMultiLineText("The text can be very Big", r20));
        aMLBox.addMultiLineText(new PLMultiLineText(" or also very Big and bold", r20b));
        aMLBox.addMultiLineText(new PLMultiLineText(" but also very small.", r6));
        aMLBox.addMultiLineText(new PLMultiLineText(" A normal size can also be chosen if you want", r10));
        aMLBox.addMultiLineText(new PLMultiLineText(", also with bold", r10b));
        aMLBox.addMultiLineText(new PLMultiLineText(" or any other font size", r14));

        aPS1.addElement(aMLBox);

        final PageLayoutPDF aPageLayout = new PageLayoutPDF();
        aPageLayout.addPageSet(aPS1);

        aPageLayout.renderTo(new File("pdf/text/multi-line-style.pdf"));
    }

    @Test
    public void testMultiTextLinesWithEmptyLine() throws PDFCreationException {
        final FontSpec r10 = new FontSpec(PreloadFont.REGULAR, 10);

        final PLMultiLineTextBox aMLBox = new PLMultiLineTextBox();
        aMLBox.addMultiLineText(new PLMultiLineText("The text can be very Big", r10));
        aMLBox.addMultiLineText(new PLMultiLineText("", r10));
        aMLBox.addMultiLineText(new PLMultiLineText(" an can have empty text.", r10));

        final PLPageSet aPS1 = new PLPageSet(PDRectangle.A4);
        aPS1.addElement(aMLBox);

        final PageLayoutPDF aPageLayout = new PageLayoutPDF();
        aPageLayout.addPageSet(aPS1);

        aPageLayout.renderTo(new File("pdf/text/multi-line-style-empty-line.pdf"));
    }

    private PLMultiLineTextBox getPLMultiLineTextBox() {
        final FontSpec r10 = new FontSpec(PreloadFont.REGULAR, 10);
        final FontSpec r10b = new FontSpec(PreloadFont.REGULAR_BOLD, 10);
        final PLMultiLineTextBox aMLBox = new PLMultiLineTextBox();
        aMLBox.addMultiLineText(new PLMultiLineText("Portable Document Format (PDF)", r10b));
        aMLBox.addMultiLineText(new PLMultiLineText(", standardized as ", r10));
        aMLBox.addMultiLineText(new PLMultiLineText("ISO 32000", r10b));
        aMLBox.addMultiLineText(new PLMultiLineText(", is a ", r10));
        aMLBox.addMultiLineText(new PLMultiLineText("file format", r10).setURI("https://en.wikipedia.org/wiki/File_format"));
        aMLBox.addMultiLineText(new PLMultiLineText(" developed by ", r10));
        aMLBox.addMultiLineText(new PLMultiLineText("Adobe", r10).setURI("https://en.wikipedia.org/wiki/Adobe_Inc."));
        aMLBox.addMultiLineText(new PLMultiLineText(" in 1992 to present ", r10));
        aMLBox.addMultiLineText(new PLMultiLineText("documents", r10).setURI("https://en.wikipedia.org/wiki/Document"));
        aMLBox.addMultiLineText(new PLMultiLineText(", including text formatting and images, in a manner independent of ", r10));
        aMLBox.addMultiLineText(new PLMultiLineText("application software", r10).setURI("https://en.wikipedia.org/wiki/Application_software"));
        aMLBox.addMultiLineText(new PLMultiLineText(", ", r10));
        aMLBox.addMultiLineText(new PLMultiLineText("hardware", r10).setURI("https://en.wikipedia.org/wiki/Computer_hardware"));
        aMLBox.addMultiLineText(new PLMultiLineText(", and ", r10));
        aMLBox.addMultiLineText(new PLMultiLineText("operating systems", r10).setURI("https://en.wikipedia.org/wiki/Operating_system"));
        aMLBox.addMultiLineText(new PLMultiLineText(".[2][3] Based on the ", r10));
        aMLBox.addMultiLineText(new PLMultiLineText("PostScript", r10).setURI("https://en.wikipedia.org/wiki/PostScript"));
        aMLBox.addMultiLineText(new PLMultiLineText(" language, each PDF file encapsulates a complete description of a fixed-layout flat document, including the text, ", r10));
        aMLBox.addMultiLineText(new PLMultiLineText("fonts", r10).setURI("https://en.wikipedia.org/wiki/Font"));
        aMLBox.addMultiLineText(new PLMultiLineText(", ", r10));
        aMLBox.addMultiLineText(new PLMultiLineText("vector graphics", r10).setURI("https://en.wikipedia.org/wiki/Vector_graphics"));
        aMLBox.addMultiLineText(new PLMultiLineText(", ", r10));
        aMLBox.addMultiLineText(new PLMultiLineText("raster images", r10).setURI("https://en.wikipedia.org/wiki/Raster_graphics"));
        aMLBox.addMultiLineText(new PLMultiLineText(" and other information needed to display it. PDF has its roots in \"The Camelot Project\" initiated by Adobe co-founder ", r10));
        aMLBox.addMultiLineText(new PLMultiLineText("John Warnock", r10).setURI("https://en.wikipedia.org/wiki/John_Warnock"));
        aMLBox.addMultiLineText(new PLMultiLineText(" in 1991.[4]", r10));

        return aMLBox;
    }

    private PLText getPLText() {
        final FontSpec r10 = new FontSpec(PreloadFont.REGULAR, 10);
        PLText text = new PLText("Portable Document Format (PDF), standardized as ISO 32000, is a file format developed by Adobe in 1992 to present documents, " +
                "including text formatting and images, in a manner independent of application software, hardware, and operating systems.[2][3] Based on the " +
                "PostScript language, each PDF file encapsulates a complete description of a fixed-layout flat document, including the text, fonts, vector " +
                "graphics, raster images and other information needed to display it. PDF has its roots in \"The Camelot Project\" initiated by Adobe co-founder " +
                "John Warnock in 1991.[4]", r10);
        return text;
    }
}
