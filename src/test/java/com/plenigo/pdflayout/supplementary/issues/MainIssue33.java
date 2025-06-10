/*
 * Copyright (C) 2014-2025 Philip Helger (www.helger.com)
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
import com.plenigo.pdflayout.base.EPLPlaceholder;
import com.plenigo.pdflayout.base.PLPageSet;
import com.plenigo.pdflayout.element.table.PLTable;
import com.plenigo.pdflayout.element.table.PLTableCell;
import com.plenigo.pdflayout.element.text.PLText;
import com.plenigo.pdflayout.spec.FontSpec;
import com.plenigo.pdflayout.spec.PreloadFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainIssue33
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainIssue33.class);

  public static void main (final String [] args) throws PDFCreationException
  {
    final PLPageSet pageSet = new PLPageSet (6 * 72, 9 * 72);
    pageSet.setMargin (75, 75, 75, 75);

    final FontSpec footerFont = new FontSpec (PreloadFont.REGULAR, 10);
    final PLText footer = new PLText ("This is the footer for page " + EPLPlaceholder.TOTAL_PAGE_NUMBER.getVariable (),
                                      footerFont);
    footer.setReplacePlaceholder (true);
    pageSet.setPageFooter (footer);

    final FontSpec clueFont = new FontSpec (PreloadFont.REGULAR, 10);
    final PLTable table = PLTable.createWithPercentage (100);
    final PLTableCell cell = new PLTableCell (new PLText ("This is a cell", clueFont));
    table.addRow (cell);
    pageSet.addElement (table);

    final PageLayoutPDF pageLayout = new PageLayoutPDF ();
    pageLayout.addPageSet (pageSet);

    // New
    pageLayout.prepareAllPageSets ();

    LOGGER.info ("Table height is " + table.getPreparedHeight ());

    // No comparison
    pageLayout.renderTo (new File ("pdf/test-issue-33.pdf"));
  }
}
