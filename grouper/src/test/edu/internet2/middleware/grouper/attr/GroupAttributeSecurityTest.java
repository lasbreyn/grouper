/**
 * Copyright 2014 Internet2
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package edu.internet2.middleware.grouper.attr;

import junit.textui.TestRunner;
import edu.internet2.middleware.grouper.Group;
import edu.internet2.middleware.grouper.GrouperSession;
import edu.internet2.middleware.grouper.Stem;
import edu.internet2.middleware.grouper.StemSave;
import edu.internet2.middleware.grouper.cfg.GrouperConfig;
import edu.internet2.middleware.grouper.exception.AttributeDefNotFoundException;
import edu.internet2.middleware.grouper.exception.InsufficientPrivilegeException;
import edu.internet2.middleware.grouper.helper.GrouperTest;
import edu.internet2.middleware.grouper.helper.SessionHelper;
import edu.internet2.middleware.grouper.helper.StemHelper;
import edu.internet2.middleware.grouper.helper.SubjectTestHelper;
import edu.internet2.middleware.grouper.privs.AccessPrivilege;
import edu.internet2.middleware.grouper.privs.AttributeDefPrivilege;
import edu.internet2.middleware.subject.Subject;

/**
 * @author mchyzer
 *
 */
public class GroupAttributeSecurityTest extends GrouperTest {

  /**
   * 
   * @param args
   */
  public static void main(String[] args) {
    TestRunner.run(new GroupAttributeSecurityTest("testGrouperSystem"));
    //TestRunner.run(GroupAttributeSecurityTest.class);
  }

  /** grouper sesion */
  private GrouperSession grouperSession;
  /** edu stem */
  private Stem edu;
  /** test group */
  private Group group;
  /** root stem */
  private Stem root;
  /** top stem */
  private Stem top;
  /** */
  private Stem etc;
  /** */
  private Group wheel;
  /** */
  private AttributeDef attributeDef1;
  /** */
  private AttributeDef attributeDef2;
  /** */
  private AttributeDefName attributeDefName1_1;
  /** */
  private AttributeDefName attributeDefName1_2;
  /** */
  private AttributeDefName attributeDefName2_1;
  /** */
  private AttributeDefName attributeDefName2_2;

  /**
   * 
   */
  public GroupAttributeSecurityTest() {
    super();
  }

  /**
   * 
   * @param name
   */
  public GroupAttributeSecurityTest(String name) {
    super(name);
  }

  /**
   * 
   * @see edu.internet2.middleware.grouper.helper.GrouperTest#setUp()
   */
  protected void setUp () {
    super.setUp();
    this.grouperSession     = SessionHelper.getRootSession();
    this.root  = StemHelper.findRootStem(grouperSession);
    this.edu   = StemHelper.addChildStem(root, "edu", "education");
    this.group = StemHelper.addChildGroup(this.edu, "group", "the group");
    this.top = this.root.addChildStem("top", "top display name");

    @SuppressWarnings("unused")
    Subject subject = SubjectTestHelper.SUBJ0;
    
    GrouperConfig.retrieveConfig().propertiesOverrideMap().put("attributeDefs.create.grant.all.attrAdmin", "false");
    GrouperConfig.retrieveConfig().propertiesOverrideMap().put("attributeDefs.create.grant.all.attrOptin", "false");
    GrouperConfig.retrieveConfig().propertiesOverrideMap().put("attributeDefs.create.grant.all.attrOptout", "false");
    GrouperConfig.retrieveConfig().propertiesOverrideMap().put("attributeDefs.create.grant.all.attrRead", "false");
    GrouperConfig.retrieveConfig().propertiesOverrideMap().put("attributeDefs.create.grant.all.attrUpdate", "false");
    GrouperConfig.retrieveConfig().propertiesOverrideMap().put("attributeDefs.create.grant.all.attrView", "false");

    GrouperConfig.retrieveConfig().propertiesOverrideMap().put("groups.create.grant.all.read", "false");
    GrouperConfig.retrieveConfig().propertiesOverrideMap().put("groups.create.grant.all.view", "false");

    this.etc = new StemSave(this.grouperSession).assignStemNameToEdit("etc").assignName("etc").save();
    this.wheel = etc.addChildGroup("wheel","wheel");
    
    GrouperConfig.retrieveConfig().propertiesOverrideMap().put("groups.wheel.use", "true");
    GrouperConfig.retrieveConfig().propertiesOverrideMap().put("groups.wheel.group", wheel.getName());
    
    this.attributeDef1 = this.top.addChildAttributeDef("test", AttributeDefType.attr);
    this.attributeDef1.setAssignToGroup(true);
    this.attributeDef1.store();
    this.attributeDef2 = this.top.addChildAttributeDef("test2", AttributeDefType.attr);
    this.attributeDef2.setAssignToGroup(true);
    this.attributeDef2.store();
  
    this.attributeDefName1_1 = this.top.addChildAttributeDefName(attributeDef1, "testName1_1", "test name1_1");
    this.attributeDefName1_2 = this.top.addChildAttributeDefName(attributeDef1, "testName1_2", "test name1_2");
    this.attributeDefName2_1 = this.top.addChildAttributeDefName(attributeDef2, "testName2_1", "test name2_1");
    this.attributeDefName2_2 = this.top.addChildAttributeDefName(attributeDef2, "testName2_2", "test name2_2");
  
    //subj0 cant groupAttrRead/groupAttrUpdate group or update the attribute (def1)
    //subj1 can groupAttrRead/groupAttrUpdate group but nothing on attribute (def1)
    //subj2 cant groupAttrRead/groupAttrUpdate group, can update attribute (def1)
    //subj3 is wheel group
    //subj4 can groupAttrRead/groupAttrUpdate group and admin attribute (def1)
    //subj5 can update/groupAttrRead group and admin attribute (def1)
    //subj6 can groupAttrRead/groupAttrUpdate group and update attribute (not read) (def1)
    //subj7 can groupAttrRead/groupAttrUpdate group and update/read attribute (def1)
    //subj8 can admin group and update/read attribute (def1)
    
    this.group.grantPriv(SubjectTestHelper.SUBJ1, AccessPrivilege.GROUP_ATTR_READ);
    this.group.grantPriv(SubjectTestHelper.SUBJ1, AccessPrivilege.GROUP_ATTR_UPDATE);
    
    this.attributeDef1.getPrivilegeDelegate().grantPriv(SubjectTestHelper.SUBJ2, AttributeDefPrivilege.ATTR_UPDATE, true);
    
    this.wheel.addMember(SubjectTestHelper.SUBJ3);

    this.group.grantPriv(SubjectTestHelper.SUBJ4, AccessPrivilege.GROUP_ATTR_READ);
    this.group.grantPriv(SubjectTestHelper.SUBJ4, AccessPrivilege.GROUP_ATTR_UPDATE);
    this.attributeDef1.getPrivilegeDelegate().grantPriv(SubjectTestHelper.SUBJ4, AttributeDefPrivilege.ATTR_ADMIN, true);
    
    this.group.grantPriv(SubjectTestHelper.SUBJ5, AccessPrivilege.UPDATE);
    this.group.grantPriv(SubjectTestHelper.SUBJ5, AccessPrivilege.GROUP_ATTR_READ);
    this.attributeDef1.getPrivilegeDelegate().grantPriv(SubjectTestHelper.SUBJ5, AttributeDefPrivilege.ATTR_ADMIN, true);
    
    this.group.grantPriv(SubjectTestHelper.SUBJ6, AccessPrivilege.GROUP_ATTR_READ);
    this.group.grantPriv(SubjectTestHelper.SUBJ6, AccessPrivilege.GROUP_ATTR_UPDATE);
    this.attributeDef1.getPrivilegeDelegate().grantPriv(SubjectTestHelper.SUBJ6, AttributeDefPrivilege.ATTR_UPDATE, true);

    this.group.grantPriv(SubjectTestHelper.SUBJ7, AccessPrivilege.GROUP_ATTR_READ);
    this.group.grantPriv(SubjectTestHelper.SUBJ7, AccessPrivilege.GROUP_ATTR_UPDATE);
    this.attributeDef1.getPrivilegeDelegate().grantPriv(SubjectTestHelper.SUBJ7, AttributeDefPrivilege.ATTR_UPDATE, true);
    this.attributeDef1.getPrivilegeDelegate().grantPriv(SubjectTestHelper.SUBJ7, AttributeDefPrivilege.ATTR_READ, true);

    this.group.grantPriv(SubjectTestHelper.SUBJ8, AccessPrivilege.ADMIN);
    this.attributeDef1.getPrivilegeDelegate().grantPriv(SubjectTestHelper.SUBJ8, AttributeDefPrivilege.ATTR_UPDATE, true);
    this.attributeDef1.getPrivilegeDelegate().grantPriv(SubjectTestHelper.SUBJ8, AttributeDefPrivilege.ATTR_READ, true);
  }

  /**
   * assign a privilege to a group
   */
  public void assignPrivilegeToGroup() {
    this.attributeDef1.getPrivilegeDelegate().grantPriv(this.group.toSubject(), AttributeDefPrivilege.ATTR_UPDATE, false);
  }
  
  /**
   * @throws Exception 
   */
  public void testGrouperSystem() throws Exception {

    //###################
    // try grouper system
    
    //assign these
    assertTrue(this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1).isChanged());
    assertTrue(this.group.getAttributeDelegate().assignAttribute(attributeDefName2_1).isChanged());
    assertTrue(this.group.getAttributeDelegate().assignAttribute(attributeDefName2_2).isChanged());
    assertFalse(this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1).isChanged());
    assertFalse(this.group.getAttributeDelegate().assignAttribute(attributeDefName2_1).isChanged());
    assertFalse(this.group.getAttributeDelegate().assignAttribute(attributeDefName2_2).isChanged());
    
    assertTrue(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2));
    assertTrue(this.group.getAttributeDelegate().hasAttribute(attributeDefName2_1));
    assertTrue(this.group.getAttributeDelegate().hasAttribute(attributeDefName2_2));
    
    assertEquals(1, this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size());
    assertEquals(2, this.group.getAttributeDelegate().retrieveAttributes(attributeDef2).size());
    assertEquals(1, this.group.getAttributeDelegate().retrieveAssignments(attributeDef1).size());
    assertEquals(2, this.group.getAttributeDelegate().retrieveAssignments(attributeDef2).size());
    assertEquals(1, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size());
    assertEquals(1, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_1).size());
    assertEquals(1, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_2).size());
    
    assertEquals(3, this.group.getAttributeDelegate().retrieveAssignments().size());
    
    
    assertTrue(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1).isChanged());
    assertFalse(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_2).isChanged());
    assertTrue(this.group.getAttributeDelegate().removeAttribute(attributeDefName2_1).isChanged());
    assertTrue(this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2).isChanged());
    assertFalse(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1).isChanged());
    assertFalse(this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2).isChanged());

    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName2_1));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName2_2));
    
    assertEquals(0, this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAttributes(attributeDef2).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDef1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDef2).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_2).size());
    
  }
  
  /**
   * subj0 cant groupAttrRead/groupAttrUpdate group or update the attribute (def1)
   * @throws Exception 
   */
  public void testSecuritySubj0() throws Exception {
  
    //###################
    // assign an attribute

    this.grouperSession.stop();
    this.grouperSession = GrouperSession.start( SubjectTestHelper.SUBJ0 );

    //subj0 cant even update/admin the attribute, or admin the group
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    //subj0 cant even update/admin the attribute, or admin the group
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }

    //############################################
    // Try to read an attribute
    
    
    try {
      this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().hasAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().hasAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size();
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAttributes(attributeDef2).size();
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDef1).size();
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDef2).size();
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size();
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size();
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_1).size();
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_2).size();
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    
    //##############################
    // remove attributes
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName1_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    
  }

  /**
   * subj1 can groupAttrRead/groupAttrUpdate group but nothing on attribute (def1)
   * @throws Exception 
   */
  public void testSecuritySubj1() throws Exception {
  
    //###################
    // assign an attribute
  
    this.grouperSession.stop();
    this.grouperSession = GrouperSession.start( SubjectTestHelper.SUBJ1 );
  
    //subj0 cant even update/admin the attribute, or admin the group
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    //subj0 cant even update/admin the attribute, or admin the group
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
  
    //############################################
    // Try to read an attribute
    
    
    try {
      this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().hasAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().hasAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size();
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAttributes(attributeDef2).size();
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDef1).size();
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDef2).size();
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size();
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size();
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_1).size();
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_2).size();
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    
    //##############################
    // remove attributes
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName1_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    
  }

  /**
   * subj2 cant groupAttrRead/groupAttrUpdate group, can update attribute (def1)
   * @throws Exception 
   */
  public void testSecuritySubj2() throws Exception {
  
    //###################
    // assign an attribute
  
    this.grouperSession.stop();
    this.grouperSession = GrouperSession.start( SubjectTestHelper.SUBJ2 );
  
    //subj0 cant even update/admin the attribute, or admin the group
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    //subj0 cant even update/admin the attribute, or admin the group
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
  
    //############################################
    // Try to read an attribute
    
    
    try {
      this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().hasAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().hasAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size();
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAttributes(attributeDef2).size();
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDef1).size();
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDef2).size();
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size();
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size();
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_1).size();
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_2).size();
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    
    //##############################
    // remove attributes
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName1_2);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    
  }

  /**
   * subj3 is wheel group
   * @throws Exception 
   */
  public void testSecuritySubj3() throws Exception {
  
    //###################
    // assign an attribute
  
    this.grouperSession.stop();
    this.grouperSession = GrouperSession.start( SubjectTestHelper.SUBJ3 );
  
    //assign these
    assertTrue(this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1).isChanged());
    assertTrue(this.group.getAttributeDelegate().assignAttribute(attributeDefName2_1).isChanged());
    assertTrue(this.group.getAttributeDelegate().assignAttribute(attributeDefName2_2).isChanged());
    assertFalse(this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1).isChanged());
    assertFalse(this.group.getAttributeDelegate().assignAttribute(attributeDefName2_1).isChanged());
    assertFalse(this.group.getAttributeDelegate().assignAttribute(attributeDefName2_2).isChanged());
    
    assertTrue(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2));
    assertTrue(this.group.getAttributeDelegate().hasAttribute(attributeDefName2_1));
    assertTrue(this.group.getAttributeDelegate().hasAttribute(attributeDefName2_2));
    
    assertEquals(1, this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size());
    assertEquals(2, this.group.getAttributeDelegate().retrieveAttributes(attributeDef2).size());
    assertEquals(1, this.group.getAttributeDelegate().retrieveAssignments(attributeDef1).size());
    assertEquals(2, this.group.getAttributeDelegate().retrieveAssignments(attributeDef2).size());
    assertEquals(1, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size());
    assertEquals(1, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_1).size());
    assertEquals(1, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_2).size());
    
    assertTrue(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1).isChanged());
    assertFalse(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_2).isChanged());
    assertTrue(this.group.getAttributeDelegate().removeAttribute(attributeDefName2_1).isChanged());
    assertTrue(this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2).isChanged());
    assertFalse(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1).isChanged());
    assertFalse(this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2).isChanged());

    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName2_1));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName2_2));
    
    assertEquals(0, this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAttributes(attributeDef2).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDef1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDef2).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_2).size());
    
    
  }

  /**
   * subj4 can groupAttrRead/groupAttrUpdate group and admin attribute (def1)
   * @throws Exception 
   */
  public void testSecuritySubj4() throws Exception {
  
    //###################
    // assign an attribute
  
    this.grouperSession.stop();
    this.grouperSession = GrouperSession.start( SubjectTestHelper.SUBJ4 );
  
    //assign these
    assertTrue(this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1).isChanged());
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    assertFalse(this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1).isChanged());
    
    assertTrue(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2));
    
    assertEquals(1, this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size());
    try {
      this.group.getAttributeDelegate().retrieveAttributes(attributeDef2);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    assertEquals(1, this.group.getAttributeDelegate().retrieveAssignments(attributeDef1).size());
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDef2);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }

    assertEquals(1, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size());
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    
    assertTrue(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1).isChanged());
    assertFalse(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_2).isChanged());
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    assertFalse(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1).isChanged());
  
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2));
    
    assertEquals(0, this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDef1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size());
    
    
  }

  /**
   * subj5 can update/groupAttrRead group and admin attribute (def1)
   * @throws Exception 
   */
  public void testSecuritySubj5() throws Exception {
  
    //###################
    // assign an attribute
  
    this.grouperSession.stop();
    this.grouperSession = GrouperSession.start( SubjectTestHelper.SUBJ5 );

    //assign these
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }

    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2));
    
    assertEquals(0, this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size());
    try {
      this.group.getAttributeDelegate().retrieveAttributes(attributeDef2);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
  
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size());
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName1_2);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
  
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2));
    
    assertEquals(0, this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDef1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size());
    
    
  }

  /**
   * subj6 can groupAttrRead/groupAttrUpdate group and update attribute (not read) (def1)
   * @throws Exception 
   */
  public void testSecuritySubj6() throws Exception {
  
    //###################
    // assign an attribute
  
    this.grouperSession.stop();
    this.grouperSession = GrouperSession.start( SubjectTestHelper.SUBJ6 );
  
    
    
    //assign these
    assertTrue(this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1).isChanged());
    assertFalse(this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1).isChanged());
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
  
    try {
      this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    
    try {
      this.group.getAttributeDelegate().retrieveAttributes(attributeDef1);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAttributes(attributeDef2);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
  
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    
    assertTrue(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1).isChanged());
    assertFalse(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_2).isChanged());
    assertFalse(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1).isChanged());

    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
  
  }

  /**
   * subj7 can groupAttrRead/groupAttrUpdate group and update/read attribute (def1)
   * @throws Exception 
   */
  public void testSecuritySubj7() throws Exception {
  
    //###################
    // assign an attribute
  
    this.grouperSession.stop();
    this.grouperSession = GrouperSession.start( SubjectTestHelper.SUBJ7 );
  
    //assign these
    assertTrue(this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1).isChanged());
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    assertFalse(this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1).isChanged());
    
    assertTrue(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2));
    
    assertEquals(1, this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size());
    try {
      this.group.getAttributeDelegate().retrieveAttributes(attributeDef2);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    assertEquals(1, this.group.getAttributeDelegate().retrieveAssignments(attributeDef1).size());
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDef2);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
  
    assertEquals(1, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size());
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    
    assertTrue(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1).isChanged());
    assertFalse(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_2).isChanged());
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    assertFalse(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1).isChanged());
  
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2));
    
    assertEquals(0, this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDef1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size());
    
    
  }
  

  /**
   * subj8 can admin group and update/read attribute (def1)
   * @throws Exception 
   */
  public void testSecuritySubj8() throws Exception {
  
    //###################
    // assign an attribute
  
    this.grouperSession.stop();
    this.grouperSession = GrouperSession.start( SubjectTestHelper.SUBJ8 );
  
    //assign these
    assertTrue(this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1).isChanged());
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().assignAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    assertFalse(this.group.getAttributeDelegate().assignAttribute(attributeDefName1_1).isChanged());
    
    assertTrue(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2));
    
    assertEquals(1, this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size());
    try {
      this.group.getAttributeDelegate().retrieveAttributes(attributeDef2);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
    assertEquals(1, this.group.getAttributeDelegate().retrieveAssignments(attributeDef1).size());
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDef2);
      fail("Not allowed");
    } catch (InsufficientPrivilegeException ipe) {
      //good
    }
  
    assertEquals(1, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size());
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().retrieveAssignments(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    
    assertTrue(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1).isChanged());
    assertFalse(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_2).isChanged());
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_1);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    try {
      this.group.getAttributeDelegate().removeAttribute(attributeDefName2_2);
      fail("Not allowed");
    } catch (AttributeDefNotFoundException adnfe) {
      //good
    }
    assertFalse(this.group.getAttributeDelegate().removeAttribute(attributeDefName1_1).isChanged());
  
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_1));
    assertFalse(this.group.getAttributeDelegate().hasAttribute(attributeDefName1_2));
    
    assertEquals(0, this.group.getAttributeDelegate().retrieveAttributes(attributeDef1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDef1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_1).size());
    assertEquals(0, this.group.getAttributeDelegate().retrieveAssignments(attributeDefName1_2).size());
    
    
  }
}
