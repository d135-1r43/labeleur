package net.herhoffer.labeleur.settings;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import net.herhoffer.labeleur.account.OwnedKey;

@ApplicationScoped
public class TapePresetRepository implements PanacheRepositoryBase<TapePreset, OwnedKey>
{

	/** A user's presets, shortest first. */
	public List<TapePreset> forUser(String userId)
	{
		return list("userId", Sort.by("lengthMm").and("readAlong"), userId);
	}
}
