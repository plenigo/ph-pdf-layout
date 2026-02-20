package com.plenigo.pdflayout;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.io.file.FileHelper;

import de.redsix.pdfcompare.CompareResult;
import de.redsix.pdfcompare.PdfComparator;
import de.redsix.pdfcompare.RenderingException;

/**
 * Test class helper to easily compare PDFs.
 *
 * @author Philip Helger
 */
@Immutable
public final class PDFTestComparer
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDFTestComparer.class);

  private PDFTestComparer ()
  {}

  public static void renderAndCompare (@NonNull final PageLayoutPDF aPageLayout, final File fTarget)
                                                                                                     throws PDFCreationException
  {
    // Render
    aPageLayout.renderTo (fTarget);

    // Get comparison file
    final File fExpected = FileHelper.getCanonicalFileOrNull (new File ("example-files/",
                                                                        StringHelper.trimStart (fTarget.getPath (),
                                                                                                "pdf")));
    assertTrue ("Non-existing PDF file '" + fExpected.getAbsolutePath () + "' to compare to", fExpected.isFile ());

    try
    {
      LOGGER.warn ("Now comparing '" + fExpected.getAbsolutePath () + "' with '" + fTarget.getAbsolutePath () + "'");
      final CompareResult aResult = new PdfComparator <> (fExpected, fTarget).compare ();
      assertTrue ("Difference in file " + fTarget.getAbsolutePath (), aResult.isEqual ());
    }
    catch (final RenderingException | IOException ex)
    {
      throw new PDFCreationException ("Failed to compare PDFs", ex);
    }
  }
}
