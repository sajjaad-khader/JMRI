// AbstractLightManagerConfigXML.java

package jmri.configurexml;

import jmri.InstanceManager;
import jmri.Light;
import jmri.LightManager;
import java.util.List;
import org.jdom.Element;

/**
 * Provides the abstract base and store functionality for
 * configuring LightManagers, working with
 * AbstractLightManagers.
 * <P>
 * Typically, a subclass will just implement the load(Element sensors)
 * class, relying on implementation here to load the individual lights.
 * Note that these are stored explicitly, so the
 * resolution mechanism doesn't need to see *Xml classes for each
 * specific Light or AbstractLight subclass at store time.
 * <P>
 * Based on AbstractSensorManagerConfigXML.java
 *
 * @author Dave Duchamp Copyright (c) 2004, 2008
 * @version $Revision: 1.14 $
 */
public abstract class AbstractLightManagerConfigXML extends AbstractNamedBeanManagerConfigXML {

    public AbstractLightManagerConfigXML() {
    }

    /**
     * Default implementation for storing the contents of a
     * LightManager
     * @param o Object to store, of type LightManager
     * @return Element containing the complete info
     */
    public Element store(Object o) {
        Element lights = new Element("lights");
        setStoreElementClass(lights);
        LightManager tm = (LightManager) o;
        if (tm!=null) {
            java.util.Iterator iter =
                                    tm.getSystemNameList().iterator();

            // don't return an element if there are not lights to include
            if (!iter.hasNext()) return null;
            
            // store the lights
            while (iter.hasNext()) {
                String sname = (String)iter.next();
                if (sname==null) log.error("System name null during store");
                log.debug("system name is "+sname);
                Light lgt = tm.getBySystemName(sname);
                Element elem = new Element("light")
                            .setAttribute("systemName", sname);

                // store common parts
                storeCommon(lgt, elem);
                
                int type = lgt.getControlType();
                elem.setAttribute("controlType", ""+type);
                if (type==Light.SENSOR_CONTROL) {
                    elem.setAttribute("controlSensor", lgt.getControlSensorName() );
                    elem.setAttribute("sensorSense", ""+lgt.getControlSensorSense() );
                }
                else if (type==Light.FAST_CLOCK_CONTROL) {
                    elem.setAttribute("fastClockOnHour", ""+lgt.getFastClockOnHour() );
                    elem.setAttribute("fastClockOnMin", ""+lgt.getFastClockOnMin() );
                    elem.setAttribute("fastClockOffHour", ""+lgt.getFastClockOffHour() );
                    elem.setAttribute("fastClockOffMin", ""+lgt.getFastClockOffMin() );
                }
                else if (type==Light.TURNOUT_STATUS_CONTROL) {
                    elem.setAttribute("controlTurnout", lgt.getControlTurnoutName() );
                    elem.setAttribute("turnoutState", ""+lgt.getControlTurnoutState() );
                }
                else if (type==Light.TIMED_ON_CONTROL) {
                    elem.setAttribute("timedControlSensor", lgt.getControlTimedOnSensorName() );
                    elem.setAttribute("duration", ""+lgt.getTimedOnDuration() );
                }

                // write variable intensity attributes
                elem.setAttribute("minIntensity", "" + lgt.getMinIntensity());
                elem.setAttribute("maxIntensity", "" + lgt.getMaxIntensity());

                // write transition attribute
                elem.setAttribute("transitionTime", "" + lgt.getTransitionTime());

                log.debug("store light "+sname);
                lights.addContent(elem);

            }
        }
        return lights;
    }

    /**
     * Subclass provides implementation to create the correct top
     * element, including the type information.
     * Default implementation is to use the local class here.
     * @param lights The top-level element being created
     */
    abstract public void setStoreElementClass(Element lights);

    /**
     * Create a LightManager object of the correct class, then
     * register and fill it.
     * @param lights Top level Element to unpack.
     */
    abstract public void load(Element lights);

    /**
     * Utility method to load the individual Light objects.
     * If there's no additional info needed for a specific light type,
     * invoke this with the parent of the set of Light elements.
     * @param lights Element containing the Light elements to load.
     */
    public void loadLights(Element lights) {
        List lightList = lights.getChildren("light");
        if (log.isDebugEnabled()) log.debug("Found "+lightList.size()+" lights");
        LightManager tm = InstanceManager.lightManagerInstance();

        for (int i=0; i<lightList.size(); i++) {
            if ( ((Element)(lightList.get(i))).getAttribute("systemName") == null) {
                log.warn("unexpected null in systemName "+((Element)(lightList.get(i)))+" "+((Element)(lightList.get(i))).getAttributes());
                break;
            }
            String sysName = ((Element)(lightList.get(i))).getAttribute("systemName").getValue();
            String userName = null;
            if ( ((Element)(lightList.get(i))).getAttribute("userName") != null)
                userName = ((Element)(lightList.get(i))).getAttribute("userName").getValue();
            if (log.isDebugEnabled()) log.debug("create light: ("+sysName+")("+
                                                            (userName==null?"<null>":userName)+")");
            Light lgt = tm.newLight(sysName, userName);
            if (lgt!=null) {
                // load common parts
                loadCommon(lgt, ((Element)(lightList.get(i))));
                
                String temString = ((Element)(lightList.get(i))).getAttribute("controlType").getValue();
                int type = Integer.parseInt(temString);
                lgt.setControlType(type);
                if (type==Light.SENSOR_CONTROL) {
                    lgt.setControlSensor(((Element)(lightList.get(i))).
                                            getAttribute("controlSensor").getValue() );
					// check for valid sensor name
					if (lgt.getControlSensorName().length()<1) {
						lgt.setControlType(Light.NO_CONTROL);
						log.warn ("invalid sensor name when loading light - "+sysName);
					}
					else {
						lgt.setControlSensorSense( Integer.parseInt(((Element)(lightList.get(i))).
                                                    getAttribute("sensorSense").getValue()) );
					}
                }
                else if (type==Light.FAST_CLOCK_CONTROL) {
                    int onHour = Integer.parseInt(((Element)(lightList.get(i))).
                                                getAttribute("fastClockOnHour").getValue());
                    int onMin = Integer.parseInt(((Element)(lightList.get(i))).
                                                getAttribute("fastClockOnMin").getValue());
                    int offHour = Integer.parseInt(((Element)(lightList.get(i))).
                                                getAttribute("fastClockOffHour").getValue());
                    int offMin = Integer.parseInt(((Element)(lightList.get(i))).
                                                getAttribute("fastClockOffMin").getValue());
                    lgt.setFastClockControlSchedule(onHour,onMin,offHour,offMin);
                }
                else if (type==Light.TURNOUT_STATUS_CONTROL) {
                    lgt.setControlTurnout(((Element)(lightList.get(i))).
                                            getAttribute("controlTurnout").getValue());
					// check for valid turnout name
					if (lgt.getControlTurnoutName().length()<1) {
						lgt.setControlType(Light.NO_CONTROL);
						log.warn ("invalid turnout name when loading light - "+sysName);
					}
					else {
						lgt.setControlTurnoutState( Integer.parseInt(((Element)(lightList.get(i))).
                                                    getAttribute("turnoutState").getValue()) );
					}
                }
                else if (type==Light.TIMED_ON_CONTROL) {
                    lgt.setControlTimedOnSensor(((Element)(lightList.get(i))).
                                            getAttribute("timedControlSensor").getValue() );
					// check for valid sensor name
					if (lgt.getControlTimedOnSensorName().length()<1) {
						lgt.setControlType(Light.NO_CONTROL);
						log.warn ("invalid timed on sensor name when loading light - "+sysName);
					}
					else {
						lgt.setTimedOnDuration( Integer.parseInt(((Element)(lightList.get(i))).
                                                    getAttribute("duration").getValue()) );
					}
                }
                // variable intensity, transition attributes
                double value = 0.0;
                try {
                    value = Double.parseDouble(((Element)(lightList.get(i))).getAttribute("minIntensity").getValue());
                } catch (Exception e1) {}
                lgt.setMinIntensity(value);

                value = 1.0;
                try {
                    value = Double.parseDouble(((Element)(lightList.get(i))).getAttribute("maxIntensity").getValue());
                } catch (Exception e2) {}
                lgt.setMaxIntensity(value);

                value = 0.0;
                try {
                    value = Double.parseDouble(((Element)(lightList.get(i))).getAttribute("transitionTime").getValue());
                } catch (Exception e2) {}
                lgt.setTransitionTime(value);
                
                // done, start it working
				lgt.activateLight();
            }
            else {
                log.error ("failed to create Light: "+sysName);
            }
        }
    }

    static org.apache.log4j.Category log = org.apache.log4j.Category.getInstance(AbstractLightManagerConfigXML.class.getName());
}