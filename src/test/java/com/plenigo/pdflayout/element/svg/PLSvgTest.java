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
package com.plenigo.pdflayout.element.svg;

import com.helger.commons.io.resource.ClassPathResource;
import com.plenigo.pdflayout.PDFCreationException;
import com.plenigo.pdflayout.PDFTestComparer;
import com.plenigo.pdflayout.PLDebugTestRule;
import com.plenigo.pdflayout.PageLayoutPDF;
import com.plenigo.pdflayout.base.PLColor;
import com.plenigo.pdflayout.base.PLPageSet;
import com.plenigo.pdflayout.element.hbox.PLHBox;
import com.plenigo.pdflayout.element.image.PLImage;
import com.plenigo.pdflayout.element.image.PLStreamImage;
import com.plenigo.pdflayout.element.text.PLText;
import com.plenigo.pdflayout.spec.BorderStyleSpec;
import com.plenigo.pdflayout.spec.EHorzAlignment;
import com.plenigo.pdflayout.spec.FontSpec;
import com.plenigo.pdflayout.spec.PreloadFont;
import com.plenigo.pdflayout.spec.WidthSpec;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Test class for {@link PLImage} and {@link PLStreamImage}
 *
 * @author plenigo
 */
public final class PLSvgTest {
    @Rule
    public final TestRule m_aRule = new PLDebugTestRule();

    @Test
    public void testBasic() throws PDFCreationException, IOException {
        final FontSpec r10 = new FontSpec(PreloadFont.REGULAR, 10);

        final PLPageSet aPS1 = new PLPageSet(PDRectangle.A4).setMargin(30);

        aPS1.addElement(new PLText("First line - left image below", r10).setHorzAlign(EHorzAlignment.CENTER).setBorder(PLColor.RED));
        aPS1.addElement(new PLSvg(ClassPathResource.getInputStream("images/testsvg1.svg").readAllBytes(), 140, 40));
        aPS1.addElement(new PLSvg(ClassPathResource.getInputStream("images/plenigo.svg").readAllBytes(), 140, 40));
        aPS1.addElement(new PLImage(ImageIO.read(ClassPathResource.getInputStream("images/test1.jpg")), 140, 40));

        aPS1.addElement(new PLText("Second line - left image below double size", r10).setHorzAlign(EHorzAlignment.CENTER).setBorder(PLColor.RED));
        aPS1.addElement(new PLSvg(ClassPathResource.getInputStream("images/testsvg1.svg").readAllBytes(), 40, 140));
        aPS1.addElement(new PLSvg(ClassPathResource.getInputStream("images/plenigo.svg").readAllBytes(), 40, 140));
        aPS1.addElement(new PLImage(ImageIO.read(ClassPathResource.getInputStream("images/test1.jpg")), 40, 140));

        aPS1.addElement(new PLText("Third line - table with 5 columns below", r10).setHorzAlign(EHorzAlignment.CENTER)
                .setBorder(new BorderStyleSpec(PLColor.BLUE)));

        final PLHBox aHBox = new PLHBox();
        aHBox.addColumn(new PLSvg(ClassPathResource.getInputStream("images/testsvg1.svg").readAllBytes(), 140, 40), WidthSpec.abs(140));
        aHBox.addColumn(new PLSvg(ClassPathResource.getInputStream("images/plenigo.svg").readAllBytes(), 140, 40), WidthSpec.abs(140));
        aHBox.addColumn(new PLImage(ImageIO.read(ClassPathResource.getInputStream("images/test1.jpg")), 140, 40), WidthSpec.abs(140));
        aPS1.addElement(aHBox);

        final PLHBox aHBox2 = new PLHBox();
        aHBox2.addColumn(new PLSvg(ClassPathResource.getInputStream("images/testsvg1.svg").readAllBytes(), 40, 140), WidthSpec.abs(40));
        aHBox2.addColumn(new PLSvg(ClassPathResource.getInputStream("images/plenigo.svg").readAllBytes(), 40, 140), WidthSpec.abs(40));
        aHBox2.addColumn(new PLImage(ImageIO.read(ClassPathResource.getInputStream("images/test1.jpg")), 40, 140), WidthSpec.abs(40));
        aPS1.addElement(aHBox2);

        final PLHBox aHBox3 = new PLHBox();
        aHBox3.addColumn(new PLSvg(ClassPathResource.getInputStream("images/testsvg1.svg").readAllBytes(), 100, 100), WidthSpec.abs(100));
        aHBox3.addColumn(new PLSvg(ClassPathResource.getInputStream("images/plenigo.svg").readAllBytes(), 100, 100), WidthSpec.abs(100));
        aHBox3.addColumn(new PLImage(ImageIO.read(ClassPathResource.getInputStream("images/test1.jpg")), 100, 100), WidthSpec.abs(100));
        aPS1.addElement(aHBox3);

        final PLHBox aHBox4 = new PLHBox();
        aHBox4.addColumn(new PLSvg(ClassPathResource.getInputStream("images/testsvg1.svg").readAllBytes(), 150, 150), WidthSpec.abs(150));
        aHBox4.addColumn(new PLSvg(ClassPathResource.getInputStream("images/plenigo.svg").readAllBytes(), 150, 150), WidthSpec.abs(150));
        aHBox4.addColumn(new PLImage(ImageIO.read(ClassPathResource.getInputStream("images/test1.jpg")), 150, 150), WidthSpec.abs(150));
        aPS1.addElement(aHBox4);

        aPS1.addElement(new PLText("Last line", r10).setHorzAlign(EHorzAlignment.CENTER).setBorder(PLColor.GREEN));

        final PageLayoutPDF aPageLayout = new PageLayoutPDF();
        aPageLayout.addPageSet(aPS1);
        PDFTestComparer.renderAndCompare (aPageLayout, new File ("pdf/plsvg/basic.pdf"));
    }
}
