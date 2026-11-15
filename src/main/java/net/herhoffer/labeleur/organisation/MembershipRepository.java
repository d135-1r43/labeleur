package net.herhoffer.labeleur.organisation;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MembershipRepository implements PanacheRepositoryBase<Membership, String>
{
	public List<Membership> of(String ownerId)
	{
		return list("ownerId", Sort.by("joinedAt").and("userId"), ownerId);
	}
}
