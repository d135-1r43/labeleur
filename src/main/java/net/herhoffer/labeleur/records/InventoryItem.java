package net.herhoffer.labeleur.records;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
public class InventoryItem extends RecordEntity
{

	/**
	 * A free property of the asset, such as size or colour. Keys are whatever
	 * the user has typed before.
	 */
	public record Attribute(@NotBlank @Size(max = 100) String key, @NotNull @Size(max = 300) String value)
	{
	}

	@NotNull
	public String assetTag = "";
	@NotNull
	public String name = "";
	@NotNull
	public String category = "";
	public String model;
	public String serial;
	@NotNull
	public String location = "";
	public String owner;
	/** ISO date, as the inventory reports it. */
	@Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
	public String purchased;

	@NotNull
	@Size(max = 50)
	@JdbcTypeCode(SqlTypes.JSON)
	public List<@Valid Attribute> attributes = new ArrayList<>();
}
