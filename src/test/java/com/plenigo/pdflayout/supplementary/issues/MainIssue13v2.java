/*
 * Copyright (C) 2014-2024 Philip Helger (www.helger.com)
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
package com.plenigo.pdflayout.supplementary.issues;

import java.io.File;

import com.plenigo.pdflayout.base.PLColor;
import com.plenigo.pdflayout.link.ELinkBorderStyle;
import com.plenigo.pdflayout.spec.LineDashPatternSpec;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;

public class MainIssue13v2
{
  public static void main (final String [] args) throws Exception
  {
    try (final PDDocument doc = new PDDocument ())
    {
      final PDPage page = new PDPage (new PDRectangle (250, 150));
      doc.addPage (page);

      try (final PDPageContentStream contentStream = new PDPageContentStream (doc, page))
      {
        final PDAnnotationLink txtLink = new PDAnnotationLink ();

        // border style
        final PDBorderStyleDictionary linkBorder = new PDBorderStyleDictionary ();
        linkBorder.setStyle (ELinkBorderStyle.DASHED.getID ());
        linkBorder.setDashStyle (LineDashPatternSpec.DASHED_3.getPatternCOSArray ());
        linkBorder.setWidth (10);
        txtLink.setBorderStyle (linkBorder);

        // Border color
        txtLink.setColor (PLColor.RED.getAsPDColor ());

        // Destination URI
        final PDActionURI action = new PDActionURI ();
        action.setURI ("https://www.helger.com");
        txtLink.setAction (action);

        // Position
        txtLink.setRectangle (new PDRectangle (10, 10, 200, 10 + 2 + 10 + 2));
        page.getAnnotations ().add (txtLink);

        // Main page content
        contentStream.beginText ();
        contentStream.newLineAtOffset (14, 14);
        contentStream.setFont (new PDType1Font (Standard14Fonts.FontName.COURIER_BOLD), 10);
        contentStream.showText ("This is linked to the outside world");
        contentStream.endText ();
      }

      // No need to save
      doc.save (new File ("target/issue13.pdf"));
    }
  }
}
