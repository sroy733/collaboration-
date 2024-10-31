package com.family_tree.familytree;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.family_tree.enums.Gender;
import com.family_tree.enums.PrivacySetting;
import com.family_tree.enums.SuggestionStatus;
import com.family_tree.enums.RelationshipType;
import com.family_tree.enums.Role;
import com.family_tree.enums.Status;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller // This means that this class is a Controller
@RequestMapping(path="/demo") // This means URL's start with /demo (after Application path)
public class MainController {

    @Autowired // This means to get the bean called userRepository
    // Which is auto-generated by Spring, we will use it to handle the data
    private UserRepository userRepository;

    @Autowired
    private FamilyTreeRepository familyTreeRepository;

    @Autowired
    private FamilyMemberRepository familyMemberRepository;

    @Autowired
    private SuggestEditRepository suggestEditRepository;

    @Autowired
    private RelationshipRepository relationshipRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private CollaborationRepository collaborationRepository;

    // User-related methods -----------------------------------------------------
    @PostMapping(path="/add") // Map ONLY POST Requests
    public @ResponseBody String addNewUser (@RequestParam String username,
                                            @RequestParam String emailAddress) {
        // @ResponseBody means the returned String is the response, not a view name
        // @RequestParam means it is a parameter from the GET or POST request

        //Ensure username and email are not left empty
        if (username == null || username.isEmpty() || emailAddress == null || emailAddress.isEmpty()) {
            return "Username and Email Address are required.";
        }

        try {
            //Create and add user to database
            User user = new User();
            user.setUsername(username);
            user.setEmail(emailAddress);
            userRepository.save(user);
            return "User Saved Successfully";
        } catch (Exception e) {
            return "Error saving user: " + e.getMessage();
        }
    }

    @GetMapping(path="/all")
    public @ResponseBody Iterable<User> getAllUsers() {
        // This returns a JSON or XML with the users
        return userRepository.findAll();
    }

    // FamilyTree-related methods --------------------------------------------------------
    @PostMapping("/createFamilyTree")
    public @ResponseBody String addFamilyTree(@RequestParam String treeName,
                                              @RequestParam PrivacySetting privacySetting,
                                              @RequestParam Integer userId) {
        //Ensure fields are not empty
        if (treeName == null || treeName.isEmpty()) {
            return "Tree name is required.";
        }
        if (privacySetting == null) {
            return "Privacy setting is required.";
        }
        if (userId == null) {
            return "User ID is required.";
        }

        try {
            //Find the user that will own the tree
            User owner = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            //Create and add tree information to the database
            FamilyTree familyTree = new FamilyTree();
            familyTree.setTreeName(treeName);
            familyTree.setPrivacySetting(privacySetting);
            familyTree.setOwner(owner);

            familyTreeRepository.save(familyTree);
            return "Family Tree Saved Successfully";
        } catch (Exception e) {
            return "Error saving family tree: " + e.getMessage();
        }
    }

    //Update family tree and privacy settings (user decides to edit tree information)
    @PostMapping("/updateFamilyTree")
    public @ResponseBody String updateFamilyTree(@RequestParam Integer treeId,
                                                 @RequestParam(required = false) String treeName,
                                                 @RequestParam(required = false) PrivacySetting privacySetting) {
        try {
            FamilyTree familyTree = familyTreeRepository.findById(treeId)
                    .orElseThrow(() -> new RuntimeException("Family tree not found"));

            if (treeName != null && !treeName.isEmpty()) {
                familyTree.setTreeName(treeName);
            }
            if (privacySetting != null) {
                familyTree.setPrivacySetting(privacySetting);
            }

            familyTreeRepository.save(familyTree);
            return "Family Tree Updated Successfully";
        } catch (Exception e) {
            return "Error updating family tree: " + e.getMessage();
        }
    }

    @GetMapping("/allFamilyTrees")
    public @ResponseBody Iterable<FamilyTree> getAllFamilyTrees() {
        return familyTreeRepository.findAll();
    }

    //Delete family tree by ID
    @PostMapping("/deleteFamilyTree")
    @Transactional // Ensures all deletions succeed or roll back together
    public @ResponseBody String deleteFamilyTree(@RequestParam Integer treeId) {
        try {
            // Delete all family members associated with this tree
            familyMemberRepository.deleteByTreeId(treeId);

            // Delete all relationships associated with this tree
            relationshipRepository.deleteByTreeId(treeId);

            // Delete all collaborations associated with this tree
            collaborationRepository.deleteByTreeId(treeId);

            // Delete the family tree itself
            familyTreeRepository.deleteById(treeId);

            return "Family Tree and all associated records deleted successfully";
        } catch (Exception e) {
            return "Error deleting family tree and associated records: " + e.getMessage();
        }
    }

    //FamilyMember-related methods --------------------------------------------------------------------
    @PostMapping("/addFamilyMember")
    public @ResponseBody String addFamilyMember(@RequestParam String name,
                                                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date birthdate,
                                                @RequestParam Gender gender,
                                                @RequestParam Integer userId,
                                                @RequestParam Integer treeId,
                                                @RequestParam Integer addedById,
                                                @RequestParam(required = false) Date deathdate,
                                                @RequestParam(required = false) String additionalInfo) {
        //Ensure required fields are not left empty (additional info can be left empty)
        if (name == null || name.isEmpty()) {
            return "Name is required.";
        }
        if (birthdate == null) {
            return "Birthdate is required.";
        }
        if (gender == null) {
            return "Gender is required.";
        }
        if (userId == null) {
            return "User ID is required.";
        }
        if (treeId == null) {
            return "Tree ID is required.";
        }
        if (addedById == null) {
            return "Added By ID is required.";
        }

        try {
            FamilyTree familyTree = familyTreeRepository.findById(treeId)
                    .orElseThrow(() -> new RuntimeException("Family tree not found"));

            User owner = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Owner not found"));

            User addedBy = userRepository.findById(addedById)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            //Create and add a family member to the tree
            FamilyMember familyMember = new FamilyMember();
            familyMember.setName(name);
            familyMember.setBirthdate(birthdate);
            familyMember.setDeathdate(deathdate);
            familyMember.setGender(gender);
            familyMember.setFamilyTree(familyTree);
            familyMember.setAddedBy(addedBy);
            familyMember.setAdditionalInfo(additionalInfo);

            familyMemberRepository.save(familyMember);
            return "Family Member Saved Successfully";
        } catch (Exception e) {
            return "Error saving family member: " + e.getMessage();
        }
    }

    //Method for updating/editing a family member
    @PostMapping("/editFamilyMember")
    public @ResponseBody String editFamilyMember(@RequestParam Integer memberId,
                                                 @RequestParam(required = false) String name,
                                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date birthdate,
                                                 @RequestParam(required = false) Gender gender,
                                                 @RequestParam(required = false) Date deathdate,
                                                 @RequestParam(required = false) String additionalInfo) {
        try {
            // Find the existing family member
            FamilyMember familyMember = familyMemberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Family member not found"));

            // Update only provided fields
            if (name != null && !name.isEmpty()) {
                familyMember.setName(name);
            }
            if (birthdate != null) {
                familyMember.setBirthdate(birthdate);
            }
            if (gender != null) {
                familyMember.setGender(gender);
            }
            if (deathdate != null) {
                familyMember.setDeathdate(deathdate);
            }
            if (additionalInfo != null && !additionalInfo.isEmpty()) {
                familyMember.setAdditionalInfo(additionalInfo);
            }

            // Save the updated family member
            familyMemberRepository.save(familyMember);
            return "Family Member Updated Successfully";
        } catch (Exception e) {
            return "Error updating family member: " + e.getMessage();
        }
    }

    // Get details of a single family member by memberId
    @GetMapping("/getFamilyMember")
    public @ResponseBody FamilyMember getFamilyMember(@RequestParam Integer memberId) {
        return familyMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Family member not found"));
    }

    // Get all family members in a specific family tree by treeId
    @GetMapping("/getFamilyMembersInTree")
    public @ResponseBody List<FamilyMember> getFamilyMembersInTree(@RequestParam Integer treeId) {
        return familyMemberRepository.findByFamilyTreeId(treeId);
    }

    // Method to delete a family member by ID, with cascading deletions for related data
    @PostMapping("/deleteFamilyMember")
    @Transactional // Ensure all deletions happen together or rollback on failure
    public @ResponseBody String deleteFamilyMember(@RequestParam Integer memberId) {
        try {
            // Delete relationships where this family member is involved
            relationshipRepository.deleteByMemberId(memberId);

            // Delete any attachments associated with this family member
            attachmentRepository.deleteByMemberId(memberId);

            // Delete any suggested edits related to this family member
            suggestEditRepository.deleteByMemberId(memberId);

            // Finally, delete the family member itself
            familyMemberRepository.deleteById(memberId);

            return "Family Member and all associated records deleted successfully";
        } catch (Exception e) {
            return "Error deleting family member and associated records: " + e.getMessage();
        }
    }

    //SuggestEdit-related methods -----------------------------------------------------
    @PostMapping("/addSuggestedEdit")
    public @ResponseBody String addSuggestedEdit(@RequestParam Integer memberId,
                                                 @RequestParam Integer suggestedById,
                                                 @RequestParam String fieldName, //field being changed (eg. name, birthdate, etc.)
                                                 @RequestParam String oldValue,
                                                 @RequestParam String newValue) {

        //Ensure required fields are not empty
        if (memberId == null) {
            return "Member ID is required.";
        }
        if (suggestedById == null) {
            return "Suggested By ID is required.";
        }
        if (fieldName == null || fieldName.isEmpty()) {
            return "Field name is required.";
        }
        if (oldValue == null || oldValue.isEmpty()) {
            return "Old value is required.";
        }
        if (newValue == null || newValue.isEmpty()) {
            return "New value is required.";
        }

        try {
            FamilyMember member = familyMemberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Family member not found"));

            User suggestedBy = userRepository.findById(suggestedById)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            //Create and add suggest edit to database
            SuggestEdit suggestedEdit = new SuggestEdit();
            suggestedEdit.setMember(member);
            suggestedEdit.setSuggestedBy(suggestedBy);
            suggestedEdit.setFieldName(fieldName);
            suggestedEdit.setOldValue(oldValue);
            suggestedEdit.setNewValue(newValue);
            suggestedEdit.setSuggestionStatus(SuggestionStatus.Pending);

            suggestEditRepository.save(suggestedEdit);
            return "Suggested Edit Saved Successfully";
        } catch (Exception e) {
            return "Error saving suggested edit: " + e.getMessage();
        }
    }

    // Method to retrieve all suggested edits
    @GetMapping("/allSuggestedEdits")
    public @ResponseBody Iterable<SuggestEdit> getAllSuggestedEdits() {
        return suggestEditRepository.findAll();
    }

    // Relationship-related methods ------------------------------------------------------------------
    @PostMapping("/addRelationship")
    public @ResponseBody String addRelationship(@RequestParam Integer treeId,
                                                @RequestParam Integer member1Id,
                                                @RequestParam Integer member2Id,
                                                @RequestParam RelationshipType relationship) {
        //Ensure required fields are not left empty
        if (treeId == null) {
            return "Tree ID is required.";
        }
        if (member1Id == null) {
            return "Member 1 ID is required.";
        }
        if (member2Id == null) {
            return "Member 2 ID is required.";
        }
        if (relationship == null) {
            return "Relationship type is required.";
        }

        try {
            FamilyTree familyTree = familyTreeRepository.findById(treeId)
                    .orElseThrow(() -> new RuntimeException("Family tree not found"));

            FamilyMember member1 = familyMemberRepository.findById(member1Id)
                    .orElseThrow(() -> new RuntimeException("Family member 1 not found"));

            FamilyMember member2 = familyMemberRepository.findById(member2Id)
                    .orElseThrow(() -> new RuntimeException("Family member 2 not found"));

            //Add relationship to the database
            Relationship rel = new Relationship(); //rel refers to relationship
            rel.setFamilyTree(familyTree);
            rel.setMember1(member1);
            rel.setMember2(member2);
            rel.setRelationship(relationship);

            relationshipRepository.save(rel);
            return "Relationship Saved Successfully";
        } catch (Exception e) {
            return "Error saving relationship: " + e.getMessage();
        }
    }

    @GetMapping("/allRelationships")
    public @ResponseBody Iterable<Relationship> getAllRelationships() {
        return relationshipRepository.findAll();
    }

    //Attachment-related methods -----------------------------------------------------------------
    @PostMapping("/addAttachment")
    public @ResponseBody String addAttachment(
            @RequestParam Integer memberId,
            @RequestParam String typeOfFile,
            @RequestParam MultipartFile fileData, // MultipartFile to handle binary data upload
            @RequestParam Integer uploadedById) {
        //Ensure required fields are not left empty
        if (memberId == null) {
            return "Member ID is required.";
        }
        if (typeOfFile == null || typeOfFile.isEmpty()) {
            return "Type of file is required.";
        }
        if (fileData == null || fileData.isEmpty()) {
            return "File data is required.";
        }
        if (uploadedById == null) {
            return "Uploaded By ID is required.";
        }

        try {
            // Find the family member to whom this attachment will be associated
            FamilyMember member = familyMemberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Family member not found"));
            // Find the user who uploaded this file
            User uploadedBy = userRepository.findById(uploadedById)
                    .orElseThrow(() -> new RuntimeException("Uploader not found"));
            // Create a new attachment
            Attachment attachment = new Attachment();
            attachment.setMember(member);
            attachment.setTypeOfFile(typeOfFile);
            attachment.setFileData(fileData.getBytes());// Set the file data from the MultipartFile input
            attachment.setUploadedBy(uploadedBy);

            attachmentRepository.save(attachment);
            return "Attachment Saved Successfully";
        } catch (IOException e) {
            return "Error reading file data: " + e.getMessage();
        } catch (Exception e) {
            return "Error saving attachment: " + e.getMessage();
        }
    }

    //Collaboration-related methods -----------------------------------------------------------------
    // Endpoint to add a collaboration directly with a specified status and role
    @PostMapping("/addCollaboration")
    public @ResponseBody String addCollaboration(@RequestParam Integer treeId,
                                                 @RequestParam Integer userId,
                                                 @RequestParam Role role,
                                                 @RequestParam Status status) {
        if (treeId == null) {
            return "Tree ID is required.";
        }
        if (userId == null) {
            return "User ID is required.";
        }
        if (role == null) {
            return "Role is required.";
        }
        if (status == null) {
            return "Status is required.";
        }

        try {
            FamilyTree familyTree = familyTreeRepository.findById(treeId)
                    .orElseThrow(() -> new RuntimeException("Family tree not found"));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Collaboration collaboration = new Collaboration();
            collaboration.setFamilyTree(familyTree);
            collaboration.setUser(user);
            collaboration.setRole(role);
            collaboration.setStatus(status);

            collaborationRepository.save(collaboration);
            return "Collaboration Saved Successfully";
        } catch (Exception e) {
            return "Error saving collaboration: " + e.getMessage();
        }
    }

    // Endpoint to retrieve all collaborations
    @GetMapping("/allCollaborations")
    public @ResponseBody Iterable<Collaboration> getAllCollaborations() {
        return collaborationRepository.findAll();
    }

    // Endpoint to invite a user to collaborate on a family tree
    @PostMapping("/inviteCollaborator")
    public @ResponseBody String inviteCollaborator(@RequestParam Integer treeId,
                                                   @RequestParam Integer userId,
                                                   @RequestParam Role role) {
        if (treeId == null || userId == null || role == null) {
            return "Tree ID, User ID, and Role are required.";
        }

        try {
            FamilyTree familyTree = familyTreeRepository.findById(treeId)
                    .orElseThrow(() -> new RuntimeException("Family tree not found"));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if a pending or accepted collaboration already exists
            Optional<Collaboration> existingCollaboration = collaborationRepository
                    .findByFamilyTreeIdAndUserId(treeId, userId);

            if (existingCollaboration.isPresent() &&
                    (existingCollaboration.get().getStatus() == Status.Pending ||
                            existingCollaboration.get().getStatus() == Status.Accepted)) {
                return "User is already invited or a collaborator.";
            }

            Collaboration collaboration = new Collaboration();
            collaboration.setFamilyTree(familyTree);
            collaboration.setUser(user);
            collaboration.setRole(role);
            collaboration.setStatus(Status.Pending);

            collaborationRepository.save(collaboration);
            return "Collaboration invitation sent successfully.";
        } catch (Exception e) {
            return "Error inviting collaborator: " + e.getMessage();
        }
    }

    // Endpoint for the invited user to accept the collaboration invite
    @PostMapping("/acceptCollaboration")
    public @ResponseBody String acceptCollaboration(@RequestParam Integer collaborationId) {
        try {
            Collaboration collaboration = collaborationRepository.findById(collaborationId)
                    .orElseThrow(() -> new RuntimeException("Collaboration not found"));

            collaboration.setStatus(Status.Accepted);
            collaborationRepository.save(collaboration);
            return "Collaboration accepted.";
        } catch (Exception e) {
            return "Error accepting collaboration: " + e.getMessage();
        }
    }

    // Endpoint for the invited user to decline the collaboration invite
    @PostMapping("/declineCollaboration")
    public @ResponseBody String declineCollaboration(@RequestParam Integer collaborationId) {
        try {
            Collaboration collaboration = collaborationRepository.findById(collaborationId)
                    .orElseThrow(() -> new RuntimeException("Collaboration not found"));

            collaboration.setStatus(Status.Declined);
            collaborationRepository.save(collaboration);
            return "Collaboration declined.";
        } catch (Exception e) {
            return "Error declining collaboration: " + e.getMessage();
        }
    }

    // Endpoint to update the role of a collaborator
    @PostMapping("/updateCollaborationRole")
    public @ResponseBody String updateCollaborationRole(@RequestParam Integer collaborationId,
                                                        @RequestParam Role newRole) {
        try {
            Collaboration collaboration = collaborationRepository.findById(collaborationId)
                    .orElseThrow(() -> new RuntimeException("Collaboration not found"));

            collaboration.setRole(newRole);
            collaborationRepository.save(collaboration);
            return "Collaboration role updated successfully.";
        } catch (Exception e) {
            return "Error updating collaboration role: " + e.getMessage();
        }
    }

    // Endpoint to retrieve collaborations for a specific family tree
    @GetMapping("/getCollaborationsByTree")
    public @ResponseBody List<Collaboration> getCollaborationsByTree(@RequestParam Integer treeId) {
        return collaborationRepository.findByFamilyTreeId(treeId);
    }

    // Endpoint to remove a collaborator
    @PostMapping("/removeCollaborator")
    @Transactional // Ensure the operation is atomic
    public @ResponseBody String removeCollaborator(@RequestParam Integer collaborationId) {
        try {
            collaborationRepository.deleteById(collaborationId);
            return "Collaborator removed successfully.";
        } catch (Exception e) {
            return "Error removing collaborator: " + e.getMessage();
        }
    }
}