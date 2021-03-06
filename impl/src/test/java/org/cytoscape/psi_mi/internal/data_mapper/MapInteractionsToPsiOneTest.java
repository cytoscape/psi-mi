package org.cytoscape.psi_mi.internal.data_mapper;

/*
 * #%L
 * Cytoscape PSI-MI Impl (psi-mi-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.psi_mi.internal.cyto_mapper.MapFromCytoscape;
import org.cytoscape.psi_mi.internal.cyto_mapper.MapToCytoscape;
import org.cytoscape.psi_mi.internal.model.Interaction;
import org.cytoscape.psi_mi.internal.schema.mi1.BibrefType;
import org.cytoscape.psi_mi.internal.schema.mi1.CvType;
import org.cytoscape.psi_mi.internal.schema.mi1.DbReferenceType;
import org.cytoscape.psi_mi.internal.schema.mi1.EntrySet;
import org.cytoscape.psi_mi.internal.schema.mi1.EntrySet.Entry.InteractionList;
import org.cytoscape.psi_mi.internal.schema.mi1.EntrySet.Entry.InteractorList;
import org.cytoscape.psi_mi.internal.schema.mi1.ExperimentType;
import org.cytoscape.psi_mi.internal.schema.mi1.InteractionElementType;
import org.cytoscape.psi_mi.internal.schema.mi1.InteractionElementType.ParticipantList;
import org.cytoscape.psi_mi.internal.schema.mi1.NamesType;
import org.cytoscape.psi_mi.internal.schema.mi1.ProteinInteractorType;
import org.cytoscape.psi_mi.internal.schema.mi1.ProteinParticipantType;
import org.cytoscape.psi_mi.internal.schema.mi1.RefType;
import org.cytoscape.psi_mi.internal.schema.mi1.XrefType;
import org.cytoscape.psi_mi.internal.util.ContentReader;
import org.cytoscape.model.NetworkTestSupport;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests MapInteractionsToPsiOne.
 *
 * @author Ethan Cerami
 */
public class MapInteractionsToPsiOneTest {
	
	private NetworkTestSupport networkTestSupport;

	@Before
	public void setUp() {
		networkTestSupport = new NetworkTestSupport();
	}
	
	/**
	 * Tests Mapper with Sample PSI Data File.
	 *
	 * @throws Exception All Exceptions.
	 */
	@Test
	public void testMapper() throws Exception {
		File file = new File("src/test/resources/testData/psi_sample1.xml");
		ContentReader reader = new ContentReader();
		String xml = reader.retrieveContent(file.toString());
		List<Interaction> interactions = new ArrayList<Interaction>();

		//  First map PSI-MI Level 1 to interaction objects
		MapPsiOneToInteractions mapper1 = new MapPsiOneToInteractions(xml, interactions);
		mapper1.doMapping();
		assertEquals(6, interactions.size());

		//  Second, map to Cytoscape objects
		CyNetwork network = networkTestSupport.getNetwork();
		MapToCytoscape mapper2 = new MapToCytoscape(network, interactions, MapToCytoscape.SPOKE_VIEW);
		mapper2.doMapping();

		//  Verify Number of Nodes and Number of Edges
		int nodeCount = network.getNodeCount();
		int edgeCount = network.getEdgeCount();
		assertEquals(7, nodeCount);
		assertEquals(6, edgeCount);

		//  Third, map back to interaction Objects
		MapFromCytoscape mapper3 = new MapFromCytoscape(network);
		mapper3.doMapping();
		interactions = mapper3.getInteractions();
		assertEquals(6, interactions.size());

		//  Fourth, map to PSI-MI Level 1
		MapInteractionsToPsiOne mapper4 = new MapInteractionsToPsiOne(interactions);
		mapper4.doMapping();

		EntrySet entrySet = mapper4.getModel();
		validateInteractors(entrySet.getEntry().get(0).getInteractorList());
		validateInteractions(entrySet.getEntry().get(0).getInteractionList());

		StringWriter writer = new StringWriter();
		JAXBContext jc = JAXBContext.newInstance("org.cytoscape.psi_mi.internal.schema.mi1");
		Marshaller marshaller = jc.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(entrySet, writer);

		//  Verify that XML indentation is turned on.
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
		                  + "<entrySet level=\"1\" version=\"1\" xmlns=\"net:sf:psidev:mi\">\n"
		                  + "    <entry>\n" + "        <interactorList>";
		assertTrue("XML Indentation Test has failed.  ", writer.toString().startsWith(expected));
	}

	/**
	 * Validates Interactor Objects.
	 *
	 * @param interactorList Castor InteractorList Object.
	 */
	private void validateInteractors(InteractorList interactorList) {
		for (ProteinInteractorType interactor : interactorList.getProteinInteractor()) {
			NamesType name = interactor.getNames();
			if ("YHR119W".equals(name.getShortLabel())) {
				assertTrue(name.getFullName().startsWith("Gene has a SET or TROMO"));
		
				ProteinInteractorType.Organism organism = interactor.getOrganism();
				assertEquals(new BigInteger("4932"), organism.getNcbiTaxId());
				assertEquals("baker's yeast", organism.getNames().getShortLabel());
				assertEquals("Saccharomyces cerevisiae", organism.getNames().getFullName());
		
				assertEquals("YHR119W", interactor.getId());
		
				XrefType xrefType = interactor.getXref();
				DbReferenceType xref = xrefType.getPrimaryRef();
				assertEquals("Entrez GI", xref.getDb());
				assertEquals("529135", xref.getId());
		
				xref = xrefType.getSecondaryRef().get(0);
				assertEquals("RefSeq GI", xref.getDb());
				assertEquals("6321911", xref.getId());
		
				String sequence = interactor.getSequence();
				assertTrue(sequence.startsWith("MNTYAQESKLRLKTKIGAD"));
				return;
			}
		}
		fail();
	}

	/**
	 * Validates Interaction Objects.
	 *
	 * @param interactionList Castor Interaction Object.
	 */
	private void validateInteractions(InteractionList interactionList) {
		InteractionElementType interaction = interactionList.getInteraction().get(3);
		InteractionElementType.ExperimentList expList = interaction.getExperimentList();
		ExperimentType expType = (ExperimentType) expList.getExperimentRefOrExperimentDescription()
		                                                 .get(0);
		BibrefType bibRef = expType.getBibref();
		XrefType xref = bibRef.getXref();
		DbReferenceType primaryRef = xref.getPrimaryRef();
		assertEquals("pubmed", primaryRef.getDb());
		assertEquals("11283351", primaryRef.getId());

		CvType cvType = expType.getInteractionDetection();
		NamesType name = cvType.getNames();
		assertEquals("classical two hybrid", name.getShortLabel());
		xref = cvType.getXref();
		primaryRef = xref.getPrimaryRef();
		assertEquals("PSI-MI", primaryRef.getDb());
		assertEquals("MI:0018", primaryRef.getId());

		ParticipantList pList = interaction.getParticipantList();
		ProteinParticipantType participant = pList.getProteinParticipant().get(0);
		RefType ref = participant.getProteinInteractorRef();
		String reference = ref.getRef();
		assertEquals("YCR038C", reference);

		//  Verify Interaction XRefs.
		xref = interaction.getXref();
		primaryRef = xref.getPrimaryRef();

		String db = primaryRef.getDb();
		String id = primaryRef.getId();
		assertEquals("DIP", db);
		assertEquals("61E", id);
	}
}
