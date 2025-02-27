package com.autodoc.scripts.groovy.pops.autoUserManagement

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.bc.user.UserService
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.user.ApplicationUser

def issueManager = ComponentAccessor.getIssueManager()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def userManager = ComponentAccessor.getUserManager()
def jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()
def loggedInUser = jiraAuthenticationContext.getLoggedInUser()

def issue = issueManager.getIssueObject(issue.key)
if (!issue) {
    return
}

def userEmailField = customFieldManager.getCustomFieldObject("customfield_18100")
if (!userEmailField) {
    return
}

def userEmail = issue.getCustomFieldValue(userEmailField)?.toString()?.replaceAll("\\s", "")
if (!userEmail) {
    return
}

ApplicationUser user = userManager.getUserByName(userEmail)
if (!user) {
    def doesUserExistsField = customFieldManager.getCustomFieldObject("customfield_28600")
    if (!doesUserExistsField) {
        return
    }

    issue.setCustomFieldValue(doesUserExistsField, "No")
    issueManager.updateIssue(loggedInUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
    return
}

def userService = ComponentAccessor.getComponent(UserService)
def updateUser = userService.newUserBuilder(user).active(false).build()
def updateUserValidationResult = userService.validateUpdateUser(updateUser)

if (!updateUserValidationResult.valid) {
    return
}

userService.updateUser(updateUserValidationResult)

user = userManager.getUserByKey(user.getKey())
if (!user.isActive()) {

    def doesUserDeactivatedField = customFieldManager.getCustomFieldObject("customfield_28601")
    if (!doesUserDeactivatedField) {
        return
    }

    issue.setCustomFieldValue(doesUserDeactivatedField, "Yes")
    issueManager.updateIssue(loggedInUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
}


