/*
  Copyright 2006-2007 The University Of Chicago
  Copyright 2006-2007 University Corporation for Advanced Internet Development, Inc.
  Copyright 2006-2007 EDUCAUSE
  
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
  
      http://www.apache.org/licenses/LICENSE-2.0
  
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package edu.internet2.middleware.ldappcTest;

import junit.framework.TestCase;

import java.util.Date;
import java.util.Set;
import java.util.Iterator;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import edu.internet2.middleware.subject.Subject;



import edu.internet2.middleware.grouper.GrouperSession;
import edu.internet2.middleware.grouper.Group;
import edu.internet2.middleware.grouper.GroupFinder;
import edu.internet2.middleware.grouper.GroupNotFoundException;
import edu.internet2.middleware.grouper.Stem;
import edu.internet2.middleware.grouper.StemFinder;
import edu.internet2.middleware.grouper.StemNotFoundException;
import edu.internet2.middleware.grouper.SessionException;
import edu.internet2.middleware.grouper.GroupModifiedAfterFilter;
import edu.internet2.middleware.grouper.QueryException;
import edu.internet2.middleware.grouper.GroupModifyException;
import edu.internet2.middleware.grouper.InsufficientPrivilegeException;
import edu.internet2.middleware.ldappc.LdappcProvisionControl;
import edu.internet2.middleware.ldappc.LdappcRuntimeException;
import edu.internet2.middleware.ldappc.ConfigManager;
import edu.internet2.middleware.ldappc.Constants;
import edu.internet2.middleware.ldappc.GroupProcessor;
import edu.internet2.middleware.ldappc.GrouperSessionControl;
import edu.internet2.middleware.ldappc.InputOptions;
import edu.internet2.middleware.ldappc.logging.DebugLog;
import edu.internet2.middleware.ldappc.logging.ErrorLog;
import edu.internet2.middleware.ldappcTest.DisplayTest;

/**
 * Class for testing creating members of a group so that other
 * LDAP provisioning tests will have data to work on.

 * @author Gil Singer 
 */
public class GrouperProvisionerLastModifyTest extends BaseTestCase
{
    /**
     * the grouper session
     */
    private GrouperSession grouperSession;

    /**
     * the group processor
     */
    private GroupProcessor groupProcessor;

    /**
     * the grouper session controller
     */
    private GrouperSessionControl grouperSessionControl;

    /**
     * a subject
     */
    private Subject subject;

    /**
     * Constructor
     */
    public GrouperProvisionerLastModifyTest(String name) 
    {
        super(name);
    }
    
    /**
     * Setup the fixture.
     */
    protected void setUp() 
    {
        DisplayTest.showRunClass(getClass().getName()); 
        GrouperSessionControl grouperSessionControl = new GrouperSessionControl();
        boolean started = grouperSessionControl.startSession("GrouperSystem");
        if (!started)
        {
            fail("Could not start grouper session");
        }
        grouperSession = grouperSessionControl.getSession();

        groupProcessor = new GroupProcessor(grouperSession);
    }

    /**
     * Tear down the fixture.
     */
    protected void tearDown() 
    {
        try
        { 
            grouperSession.stop();
        }
        catch (SessionException se) 
        {
            fail("Could not stop the session: " + se.getMessage());
        }
    }

    /**
     * The main method for running the test.
     */
    public static void main(String args[]) 
    {
        BaseTestCase.runTestRunner(GrouperProvisionerLastModifyTest.class);
    }

    /**
     * A test of making an update to a group and verifying that the
     * lastModifyTime is changed
     */
    public void testLastModifyTimeRetrievalAndStemQuery() 
    {
        DisplayTest.showRunTitle("test simple lastModifyTime and StemQuery");
        
        String[] args = {"-subject", "GrouperSystem", "-groups"};
        InputOptions options = new InputOptions(args);
        
        boolean success = doProvisioning(options);

        if (!success)
        {
            fail("Provisioning of groups failed to complete.");
        }
     }

     /**
      * Do the provisioning
      * @return false if fails 
      */
    private boolean doProvisioning(InputOptions options)
     {
        boolean success = false;
        if (options.isFatal())
        {
            String msg = "A fatal error occurred in Ldappc -- see the error log file";
            DebugLog.info(this.getClass(), msg);
            ErrorLog.warn(this.getClass(), "A fatal error occurred in Ldappc, check earlier messages.");
        }     
        else
        {
            //
            // Create provision control
            //
            LdappcProvisionControl pc = new LdappcProvisionControl(options);

            // Set last time huge (1000 days) to get all groups

            Date longAgo = new Date();
            longAgo.setTime(1000L*60L*60L*24L*1000L);
            int allGroupsSize = getNumberOfGroupsModifiedSinceTime(longAgo);

            assertEquals("Number of modified groups for long test is bad.", 1, allGroupsSize);

            Date justBeforeTimeOfMod = new Date();
            Group testGroup31 = null;
            try
            {
                testGroup31 = GroupFinder.findByName(grouperSession, "testStem1:testStem2:testStem3:testGroup31");
            }
            catch (GroupNotFoundException gnfe) 
            {
                ErrorLog.error(getClass(), "Group not found: " + gnfe.getMessage());
            }
            
            // Do simple modification instead of above, just change the group description.

            String oldDesc = testGroup31.getDescription();
            String newDesc = oldDesc + "."; 
            try
            {
                testGroup31.setDescription(newDesc);
            }
            catch (GroupModifyException gme)
            {
                ErrorLog.error(getClass(), "Could not modify testGroup31. " + gme.getMessage());
            }
            catch (InsufficientPrivilegeException ipe)
            {
                ErrorLog.error(getClass(), "Could not modify testGroup31. " + ipe.getMessage());
            }
            

            // The time of last modification of the group should now be the current time.

            //
            // Verify that groups were modified after the time right before the modification.
            //
            
            int selectedGroupsSize = getNumberOfGroupsModifiedSinceTime(justBeforeTimeOfMod);

            assertEquals("Number of modified groups for current time test is bad.", 1, selectedGroupsSize);

            //
            // Set the lastModifyTime in the options to just before the modification
            //
            //Calls by reflection: options.setLastModifyTime(justBeforeTimeOfMod);
            setLastModifyTime(options, justBeforeTimeOfMod);

            //
            // Recreate provision control with lastModifyTime set.
            //
            pc = new LdappcProvisionControl(options);

            //
            // Do provision again; this should do provisioning.
            //

            pc.run();

            //
            // Check that no groups were modified after the current time.
            //
            
            selectedGroupsSize = getNumberOfGroupsModifiedSinceTime(new Date());

            assertEquals("Number of modified groups for current time test is bad.", 0, selectedGroupsSize);

            success = true;
        }
        return success;
    }
    
    /**
     * Get the number of groups that have been modified more recently
     * than the current time minus the time indicated by the argument, earlier. 
     *
     * @param when The date to use for the lastModifyTime value locally for the test.
     * @return the number of groups that have been modified more recently
     * than the current time minus the time indicated by the argument, earlier. 
     */
    private int getNumberOfGroupsModifiedSinceTime(Date when)
    {
        ConfigManager configManager = ConfigManager.getInstance();
        //
        // Test getting all groups modified since the date: when
        //

        Stem stem = null;
        String stemName = "testStem1:testStem2:testStem3";
        try
        {
            stem = StemFinder.findByName(grouperSession, stemName);
        }
        catch(StemNotFoundException snfe)
        {
            ErrorLog.error(getClass(), "Stem not found: " + snfe.getMessage());
        }
        GroupModifiedAfterFilter groupModifiedAfterFilter = new GroupModifiedAfterFilter(when, stem);
        Set groupsModified = null;
        Date dateModified = null;
        try
        {
             groupsModified = groupModifiedAfterFilter.getResults(grouperSession);
             Group group = null;
             String name = null;
             Iterator it = groupsModified.iterator(); 
             while (it.hasNext())
             {
                 group = (Group) it.next();
                 name = group.getName();
                 dateModified = group.getModifyTime();
             }
        }
        catch (QueryException qe)
        {
            ErrorLog.error(getClass(), "Error testing last modify time for grouper"
                   + "\n" + qe.getMessage()); 
        }
        
        //
        // Restrict search scope to reduce execution time.
        // override ldappc.xml file to require the groups to be in a particular stem.
        //
        Set stemQueries = configManager.getGroupSubordinateStemQueries();
        String stemQuery = null;

        Iterator it = stemQueries.iterator();
        while (it.hasNext())
        {
            stemQuery = (String)it.next();
        }

        //
        // Use reflection to call: configManager.resetGroupSubordinateStemQueries();
        //
        String fullClassName = "edu.internet2.middleware.ldappc.ConfigManager";
        String methodName = "resetGroupSubordinateStemQueries";
        Class[] formalParams = null;
        ConfigManager invokingInstance = ConfigManager.getInstance();
        String[] actualParams = null;
        invokeMethod(fullClassName, methodName, formalParams, invokingInstance, actualParams);

        //
        // Use reflection to call: configManager.addGroupSubordinateStemQuery(stemName);
        //
        methodName = "addGroupSubordinateStemQuery";
        formalParams = new Class[1];
        formalParams[0] = String.class;
        actualParams = new String[1];
        actualParams[0] = stemName;
        invokeMethod(fullClassName, methodName, formalParams, invokingInstance, actualParams);

        Iterator it2 = stemQueries.iterator();
        while (it2.hasNext())
        {
            stemQuery = (String)it2.next();
        }
        return groupsModified.size(); 
    }

    /**
     * Use reflection to set the lastModifyTime.
     */
    private void setLastModifyTime(InputOptions options, Date lastModifyTime)
    {
        //
        // Use reflection to call: options.setLastModifyTime(lastModifyTime);
        //
        String fullClassName = "edu.internet2.middleware.ldappc.InputOptions";
        String methodName = "setLastModifyTime";
        Class[] formalParams = new Class[1];
        formalParams[0] = Date.class;
        InputOptions invokingInstance = options;
        //actualParams = new Date[];
        Object[] actualParams = null;
        actualParams = new Object[1];
        actualParams[0] = lastModifyTime;
        invokeMethod(fullClassName, methodName, formalParams, options, actualParams);
    }

    /**
     * Use reflection to invoke a method.
     */
    private void invokeMethod(String fullClassName, String methodName, Class[] formalParams, 
            Object invokingInstance, Object[] actualParams)
    {
        Method method = null;
        try
        {
            method = Class.forName(fullClassName)
                .getDeclaredMethod(methodName, formalParams);
            method.setAccessible(Constants.ACCESSIBILITY);
            method.invoke(invokingInstance, actualParams);
        }
        catch(ClassNotFoundException cnfe)
        {
            throw new LdappcRuntimeException("Program Error in GrouperProvisionerLastModifyTest. " 
                    + cnfe.getMessage());
        }
        catch(NoSuchMethodException nsme)
        {
            throw new LdappcRuntimeException("Program Error in GrouperProvisionerLastModifyTest. " 
                    + nsme.getMessage());
        }
        catch(IllegalAccessException iae)
        {
            throw new LdappcRuntimeException("Program Error in GrouperProvisionerLastModifyTest. " 
                    + iae.getMessage());
        }
        catch(IllegalArgumentException iae2)
        {
            throw new LdappcRuntimeException("Program Error in GrouperProvisionerLastModifyTest. "
                   + iae2.getMessage());
        }
        catch(InvocationTargetException ite)
        {
            throw new LdappcRuntimeException("Program Error in GrouperProvisionerLastModifyTest. " 
                    + ite.getMessage());
        }
    }
}
