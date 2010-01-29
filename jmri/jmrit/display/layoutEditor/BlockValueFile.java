// BlockValueFile.java

package jmri.jmrit.display.layoutEditor;

import jmri.jmrit.XmlFile;
import java.io.File;
import jmri.BlockManager;
import jmri.Block;

import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Attribute;

/**
 * Handle saving/restoring block value information to XML files.
 * This class manipulates files conforming to the block_value DTD.
 *
 * @author      Dave Duchamp Copyright (C) 2008
 * @version     $Revision: 1.1 $
 */

public class BlockValueFile extends jmri.jmrit.XmlFile {
	
	public BlockValueFile () {
		super();
		blockManager = jmri.InstanceManager.blockManagerInstance();
	}
	
	// operational variables
	private BlockManager blockManager = null;
	private static String defaultFileName = XmlFile.prefsDir()+"blockvalues.xml";
	private Document doc = null;
	private Element root = null;
	
	/*
	 *  Reads Block values from a file in the user's preferences directory
	 *  If the file containing block values does not exist this routine returns quietly.
	 *  If a Block named in the file does not exist currently, that entry is quietly ignored.
	 */
	@SuppressWarnings("unchecked")
	public void readBlockValues() throws org.jdom.JDOMException, java.io.IOException {
		log.debug("entered readBlockValues");
		List<String> blocks = blockManager.getSystemNameList();
		// check if file exists
		if (checkFile(defaultFileName)) {
			// file is present, 
			root = rootFromName(defaultFileName);
			if ( (root!=null) && (blocks!=null) && (blocks.size()>0) ) {
				// there is a file and there are Blocks defined
				Element blockvalues = root.getChild("blockvalues");
				if (blockvalues!=null) {	
					// there are values defined, read and set block values if Block exists.
					List<Element> blockList = blockvalues.getChildren("block");
					for (int i=0; i<blockList.size(); i++) {
						if ((blockList.get(i)).getAttribute("systemname") == null) {
							log.warn("unexpected null in systemName "+
									blockList.get(i)+" "+
										blockList.get(i).getAttributes());
							break;
						}
						String sysName = blockList.get(i).
												getAttribute("systemname").getValue();
						// get Block - ignore entry if block not found
						Block b = blockManager.getBySystemName(sysName);
						if (b!=null) {
							// Block was found, set its value
							String v = blockList.get(i).
												getAttribute("value").getValue();
							b.setValue(v);
							// set direction if there is one
							int dd = jmri.Path.NONE;
							Attribute a = blockList.get(i).getAttribute("dir");
							if (a!=null) {
								try {
									dd = a.getIntValue();
								}
								catch (org.jdom.DataConversionException e) {
									log.error("failed to convert direction attribute");
								}
							}
							b.setDirection(dd);
						}
					}
				}
			}
		} 
	}
	
	/*
	 *  Writes out block values to a file in the user's preferences directory
	 *  If there are no defined Blocks, no file is written.
	 *  If none of the defined Blocks have values, no file is written.
	 */
	public void writeBlockValues() throws java.io.IOException {
		log.debug("entered writeBlockValues");
		List<String> blocks = blockManager.getSystemNameList();
		if (blocks.size()>0) {
			// there are blocks defined, create root element
			root = new Element("block_values");
			doc = newDocument(root, dtdLocation+"block-values.dtd");
			boolean valuesFound = false;
			
			// add XSLT processing instruction
			// <?xml-stylesheet type="text/xsl" href="XSLT/block-values.xsl"?>
			java.util.Map<String,String> m = new java.util.HashMap<String,String>();
			m.put("type", "text/xsl");
			m.put("href", xsltLocation+"blockValues.xsl");
			org.jdom.ProcessingInstruction p = new org.jdom.ProcessingInstruction("xml-stylesheet", m);
			doc.addContent(0,p);
			
			// save block values in xml format
			Element values = new Element("blockvalues");
			for (int i = 0; i<blocks.size(); i++) {
				String sname = blocks.get(i);
				Block b = blockManager.getBySystemName(sname);
				if (b!=null) {
					Object o = b.getValue();
					if (o!=null) {
						// block has value, save it
						Element val = new Element("block");
						val.setAttribute("systemname", sname);
						val.setAttribute("value",o.toString());
						int v = b.getDirection();
						if (v!=jmri.Path.NONE) val.setAttribute("dir",""+v);
						values.addContent(val);
						valuesFound = true;
					}
				}
				else log.error("Block "+sname+" was not found.");
			}
			root.addContent(values);
			
			// write out the file if values were found
			if (valuesFound) {		
				try {
					if (!checkFile(defaultFileName)) {
						// file does not exist, create it
						File file = new File(defaultFileName);
						file.createNewFile();
					}
					// write content to file
					writeXML(findFile(defaultFileName),doc);
				}
				catch (java.io.IOException ioe) {
					log.error("IO Exception "+ioe);
					throw (ioe);
				}
			}
		}
	}
	
		       
    // initialize logging
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BlockValueFile.class.getName());

}
	

