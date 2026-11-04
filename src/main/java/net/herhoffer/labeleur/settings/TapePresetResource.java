package net.herhoffer.labeleur.settings;

import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.herhoffer.labeleur.account.CurrentUser;
import net.herhoffer.labeleur.account.OwnedKey;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestPath;

@Path("/settings/tape-presets")
@Tag(name = "Settings")
public class TapePresetResource
{

	/**
	 * What the label panel offers: a cut length within what the printers cut,
	 * and a direction.
	 */
	public record Preset(String id, @Min(25) @Max(300) int lengthMm, boolean readAlong)
	{

		static Preset of(TapePreset p)
		{
			return new Preset(p.id, p.lengthMm, p.readAlong);
		}
	}

	private final TapePresetRepository presets;
	private final CurrentUser user;

	TapePresetResource(TapePresetRepository presets, CurrentUser user)
	{
		this.presets = presets;
		this.user = user;
	}

	@GET
	@Operation(summary = "The user's continuous-tape presets, shortest first")
	public List<Preset> all()
	{
		return presets.forUser(user.id()).stream().map(Preset::of).toList();
	}

	@POST
	@Transactional
	@Operation(summary = "Keep a cut length and direction; keeping one that exists returns it unchanged")
	public Preset add(@Valid @NotNull Preset preset)
	{
		String owner = user.id();
		String id = TapePreset.idOf(preset.lengthMm(), preset.readAlong());
		TapePreset p = presets.findById(new OwnedKey(owner, id));
		if (p == null)
		{
			p = new TapePreset();
			p.userId = owner;
			p.id = id;
			p.lengthMm = preset.lengthMm();
			p.readAlong = preset.readAlong();
			presets.persist(p);
		}
		return Preset.of(p);
	}

	@DELETE
	@Path("/{id}")
	@Transactional
	@Operation(summary = "Forget a preset")
	public void remove(@RestPath String id)
	{
		if (!presets.deleteById(new OwnedKey(user.id(), id))) throw new NotFoundException("No preset " + id);
	}
}
