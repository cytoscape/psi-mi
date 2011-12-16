package org.cytoscape.psi_mi.internal.plugin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.util.StreamUtil;
import org.junit.Before;
import org.junit.Test;

public class PsiMiCyFileFilterTest {
	private CyFileFilter filter1;
	private CyFileFilter filter25;


	@Before
	public void setUp() {
		StreamUtil streamUtil = new StreamUtil() {
			
			@Override
			public URLConnection getURLConnection(URL source) throws IOException {
				return null;
			}
			
			@Override
			public InputStream getInputStream(URL source) throws IOException {
				return source.openStream();
			}
		};
		filter1 = new PsiMiCyFileFilter("PSI 1", streamUtil, PsiMiCyFileFilter.PSIMIVersion.PXIMI10);
		filter25 = new PsiMiCyFileFilter("PSI 25", streamUtil, PsiMiCyFileFilter.PSIMIVersion.PSIMI25);
	}
	
	@Test
	public void testAcceptPsiMi1() throws Exception {
		File file = new File("src/test/resources/testData/psi_sample1.xml");
		assertTrue(filter1.accepts(new FileInputStream(file), DataCategory.NETWORK));
		assertTrue(filter1.accepts(file.toURI(), DataCategory.NETWORK));
	}
	
	@Test
	public void testAcceptPsiMi25() throws Exception {
		File file = new File("src/test/resources/testData/psi_sample_2_5_2.xml");
		assertTrue(filter25.accepts(new FileInputStream(file), DataCategory.NETWORK));
		assertTrue(filter25.accepts(file.toURI(), DataCategory.NETWORK));
	}
	
	@Test
	public void testAcceptRandomXml() throws Exception {
		File file = new File("src/test/resources/testData/galFiltered.xgmml");
		assertFalse(filter25.accepts(new FileInputStream(file), DataCategory.NETWORK));
		assertFalse(filter25.accepts(file.toURI(), DataCategory.NETWORK));
	}
}
