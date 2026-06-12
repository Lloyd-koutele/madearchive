package made.archive.service.document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import made.archive.entite.*;
import made.archive.exception.BusinessException;
import made.archive.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupeAccessService
{
    private final GroupeAccessRepository groupeAccessRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    /**
     * Liste tous les membres ayant accès au document.
     * Vérifie que le demandeur est bien membre du groupe.
     */
    @Transactional(readOnly = true)
    public List<User> getMembres(UUID documentId, UUID demandeurId)
    {
        Document document = getDocumentPrive(documentId);
        verifierMembre(document.getGroupe(), demandeurId);
        return document.getGroupe().getMembres();
    }

    /**
     * Ajoute un membre au groupe du document.
     * Vérifie que le demandeur est bien membre du groupe.
     */
    @Transactional
    public void ajouterMembre(UUID documentId, UUID demandeurId, UUID nouveauMembreId)
    {
        Document document = getDocumentPrive(documentId);
        GroupeAccess groupe = document.getGroupe();
        verifierMembre(groupe, demandeurId);

        // Vérifier que le nouveau membre existe
        User nouveauMembre = userRepository.findById(nouveauMembreId)
            .orElseThrow(() -> new BusinessException(
                "Utilisateur introuvable : " + nouveauMembreId));

        // Vérifier qu'il n'est pas déjà membre
        boolean dejaPresent = groupe.getMembres().stream()
            .anyMatch(m -> m.getId().equals(nouveauMembreId));
        if (dejaPresent)
        {
            throw new BusinessException(
                "Cet utilisateur est déjà membre du groupe");
        }

        groupe.getMembres().add(nouveauMembre);
        groupeAccessRepository.save(groupe);
        log.info("[Groupe] Membre {} ajouté au groupe {} par {}",
                 nouveauMembreId, groupe.getId(), demandeurId);
    }

    /**
     * Retire un membre du groupe du document.
     * Si le groupe devient vide → suppression du groupe
     * et le document passe en PUBLIC.
     */
    @Transactional
    public void retirerMembre(UUID documentId, UUID demandeurId, UUID membreARetirerID)
    {
        Document document = getDocumentPrive(documentId);
        GroupeAccess groupe = document.getGroupe();
        verifierMembre(groupe, demandeurId);

        // Vérifier que le membre à retirer est bien dans le groupe
        boolean estMembre = groupe.getMembres().stream()
            .anyMatch(m -> m.getId().equals(membreARetirerID));
        if (!estMembre)
        {
            throw new BusinessException(
                "Cet utilisateur n'est pas membre du groupe");
        }

        groupe.getMembres().removeIf(m -> m.getId().equals(membreARetirerID));

        if (groupe.getMembres().isEmpty())
        {
            // Groupe vide → document devient public
            log.info("[Groupe] Groupe {} vide → document {} passe en PUBLIC",
                     groupe.getId(), documentId);
            document.setGroupe(null);
            document.setAccess(TypeAccess.PUBLIC);
            documentRepository.save(document);
            groupeAccessRepository.delete(groupe);
        }
        else
        {
            groupeAccessRepository.save(groupe);
            log.info("[Groupe] Membre {} retiré du groupe {} par {}",
                     membreARetirerID, groupe.getId(), demandeurId);
        }
    }

    /**
     * Liste tous les utilisateurs de la plateforme
     * qui ne sont pas encore membres du groupe.
     * Utilisé pour le formulaire d'ajout de membre.
     */
    @Transactional(readOnly = true)
    public List<User> getUtilisateursDisponibles(UUID documentId, UUID demandeurId)
    {
        Document document = getDocumentPrive(documentId);
        verifierMembre(document.getGroupe(), demandeurId);

        List<UUID> membresIds = document.getGroupe().getMembres().stream()
            .map(User::getId)
            .toList();

        // Retourne tous les users sauf ceux déjà membres
        return userRepository.findAll().stream()
            .filter(u -> !membresIds.contains(u.getId()))
            .toList();
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------

    private Document getDocumentPrive(UUID documentId)
    {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new BusinessException(
                "Document introuvable : " + documentId));

        if (document.getAccess() != TypeAccess.PRIVE || document.getGroupe() == null)
        {
            throw new BusinessException(
                "Ce document n'est pas un document privé");
        }

        return document;
    }

    private void verifierMembre(GroupeAccess groupe, UUID userId)
    {
        boolean estMembre = groupe.getMembres().stream()
            .anyMatch(m -> m.getId().equals(userId));
        if (!estMembre)
        {
            throw new BusinessException(
                "Accès refusé : vous n'êtes pas membre de ce groupe");
        }
    }
}