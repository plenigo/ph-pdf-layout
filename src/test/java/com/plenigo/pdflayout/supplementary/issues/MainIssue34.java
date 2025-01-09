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

import com.plenigo.pdflayout.PDFCreationException;
import com.plenigo.pdflayout.PageLayoutPDF;
import com.plenigo.pdflayout.base.PLColor;
import com.plenigo.pdflayout.base.PLPageSet;
import com.plenigo.pdflayout.element.box.PLBox;
import com.plenigo.pdflayout.element.table.EPLTableGridType;
import com.plenigo.pdflayout.element.table.PLTable;
import com.plenigo.pdflayout.element.table.PLTableCell;
import com.plenigo.pdflayout.element.text.PLText;
import com.plenigo.pdflayout.spec.BorderStyleSpec;
import com.plenigo.pdflayout.spec.FontSpec;
import com.plenigo.pdflayout.spec.PreloadFont;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

public class MainIssue34
{
  public static void main (final String [] args) throws PDFCreationException
  {
    final FontSpec r10 = new FontSpec (PreloadFont.REGULAR, 10);
    final FontSpec r13 = new FontSpec (PreloadFont.REGULAR, 13);
    final PLPageSet aPS1 = new PLPageSet (PDRectangle.A4);

    final PLTable aTable = PLTable.createWithEvenlySizedColumns (4);
    // header
    aTable.addRow (new PLTableCell(new PLText("Col1", r13)),
                   new PLTableCell (new PLText ("Col2", r13)),
                   new PLTableCell (new PLText ("Col3", r13)),
                   new PLTableCell (new PLText ("Col4", r13)));

    // body
    final String s = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum.";

    aTable.addRow (new PLTableCell (new PLText (s, r10)),
                   new PLTableCell (new PLText (s, r10)).setMaxHeight (65),
                   new PLTableCell (new PLText (s, r10)).setMaxHeight (75).setClipContent (true),
                   new PLTableCell (new PLText (s, r10)));

    EPLTableGridType.FULL.applyGridToTable (aTable, new BorderStyleSpec(PLColor.RED));

    aPS1.addElement (aTable);

    aPS1.addElement (new PLBox(new PLText (s, r10)).setMaxHeight (17)
                                                    .setBorder (new BorderStyleSpec (PLColor.GREEN))
                                                    .setClipContent (true));

    final PageLayoutPDF aPageLayout = new PageLayoutPDF ().setCompressPDF (false);
    aPageLayout.addPageSet (aPS1);

    aPageLayout.renderTo (new File ("target/issue34.pdf"));
  }

}
