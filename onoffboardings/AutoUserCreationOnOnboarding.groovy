package com.autodoc.scripts.groovy.pops.autoUserManagement

import com.atlassian.jira.bc.user.UserService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.user.ApplicationUser

import java.security.SecureRandom
import java.util.Collections

def userManager = ComponentAccessor.getUserManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def userService = ComponentAccessor.getComponent(UserService)
def userUtil = ComponentAccessor.userUtil

MutableIssue issue = ComponentAccessor.getIssueManager().getIssueObject(issue.key)

Map<String, List<String>> EMPLOYEE_POSITION_GROUP_MAPPING = [
        "Business Analyst"                         : ["BA"],
        "Data Analyst"                             : ["Data Analyst"],
        "Data Analyst 5"                           : ["Data Analyst"],
        "Director Data Analytics"                  : ["Data Analyst"],
        "Team Lead Data Analytics"                 : ["Data Analyst", 'jira-IT-TL-approvers'],
        "Manager Scrum"                            : ["SM"],
        "Scrum Master"                             : ["SM"],
        "Product Owner"                            : ["PO"],
        "Team Lead Product Ownership"              : ["PO"],
        "Technical Writer"                         : ["Tech Writer"],
        "Software Developer Front-End"             : ["Frontend Developers"],
        "Software Developer Android"               : ["Backend Developers"],
        "Software Developer Back-End"              : ["Backend Developers"],
        "Software Developer GO"                    : ["Backend Developers"],
        "Software Developer IOS"                   : ["Backend Developers"],
        "Software Engineer Back-End"               : ["Backend Developers"],
        "Team Lead Back-End Development"           : ["Backend Developers", 'jira-IT-TL-approvers'],
        "QA"                                       : ["QA"],
        "QA Engineer Mobile Automation 4"          : ["QA"],
        "Senior Manager QA Mobile Manual"          : ["QA"],
        "QA Engineer Web Automation"               : ["QA", "QA Web"],
        "QA Engineer Web Manual"                   : ["QA", "QA Web"],
        "Manager Back-End Development"             : ['Backend Developers', 'jira-IT-TL-approvers'],
        'Senior Manager Engineering'               : ['jira-IT-TL-approvers'],
        'Head of Business Development'             : ['jira-IT-TL-approvers'],
        'Head of Information Technology Management': ['jira-IT-TL-approvers']
]

def userCF = customFieldManager.getCustomFieldObject("customfield_19000")
def newUserEmail = (String) issue.getCustomFieldValue(customFieldManager.getCustomFieldObject("customfield_18100"))
def newUserName = (String) issue.getCustomFieldValue(customFieldManager.getCustomFieldObject("customfield_17702"))
def position = (String) issue.getCustomFieldValue(customFieldManager.getCustomFieldObject("customfield_17706"))

if (newUserEmail != null) {
    newUserEmail = newUserEmail.replaceAll("\\s", "")
    assert newUserEmail: "Value of newUserEmail is $newUserEmail"

    newUserName = newUserName.replaceAll(/[^A-Za-zĄąĆćĘęŁłŃńÓóŚśŹźŻżÄäÖöÜüßÁáÉéÍíÓóÚúÜüÑñÂâÃãÊêÍíÔôÕõÇçČčĎďĚěŇňŘřŠšŤťÚúŮůÝýŽžÀàÆæËëÏïŒœŸÿ\s]/, '').trim();

    def newUser = userManager.getUserByName(newUserEmail)
    if (newUser == null) {
        def randomPassword = generateRandomPassword()
        def adminUser = userManager.getUserByName("internal_team")

        final boolean sendNotification = true // True to send
        def newCreateRequest = UserService.CreateUserRequest.withUserDetails(adminUser, newUserEmail, randomPassword, newUserEmail, newUserName).sendNotification(sendNotification)

        def createValidationResult = userService.validateCreateUser(newCreateRequest)
        assert createValidationResult.isValid(): createValidationResult.errorCollection

        newUser = userService.createUser(createValidationResult)

        userUtil.addUserToGroup(ComponentAccessor.groupManager.getGroup("confluence-users"), newUser)

        if (position && EMPLOYEE_POSITION_GROUP_MAPPING.containsKey(position)) {
            EMPLOYEE_POSITION_GROUP_MAPPING[position].each { groupName ->
                def group = ComponentAccessor.groupManager.getGroup(groupName)
                if (group) {
                    userUtil.addUserToGroup(group, newUser)
                }
            }
        }

        issue.setCustomFieldValue(userCF, newUser)

        ComponentAccessor.getIssueManager().updateIssue(adminUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
    }
}

String generateRandomPassword() {
    String upperCaseChars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
    String lowerCaseChars = 'abcdefghijklmnopqrstuvwxyz'
    String digitChars = '0123456789'
    String specialChars = '!@#$%&*()-_=+'

    SecureRandom random = new SecureRandom()
    def password = []

    2.times { password << upperCaseChars.charAt(random.nextInt(upperCaseChars.length())) }
    15.times { password << lowerCaseChars.charAt(random.nextInt(lowerCaseChars.length())) }
    3.times { password << digitChars.charAt(random.nextInt(digitChars.length())) }
    password << specialChars.charAt(random.nextInt(specialChars.length()))

    Collections.shuffle(password, random)
    return password.join('')
}


