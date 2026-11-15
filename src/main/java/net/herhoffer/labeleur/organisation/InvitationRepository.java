package net.herhoffer.labeleur.organisation;

import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InvitationRepository implements PanacheRepositoryBase<Invitation, String>
{
	public List<Invitation> of(String ownerId)
	{
		return list("ownerId", Sort.by("createdAt"), ownerId);
	}

	public Optional<Invitation> find(String ownerId, String email)
	{
		return find("ownerId = ?1 and email = ?2", ownerId, email).firstResultOptional();
	}
}
