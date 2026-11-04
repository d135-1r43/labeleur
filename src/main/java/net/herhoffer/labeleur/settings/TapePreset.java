package net.herhoffer.labeleur.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import net.herhoffer.labeleur.account.OwnedKey;

/**
 * A cut length and text direction for continuous tape that a user keeps at
 * hand. Its id is made of both, so the same preset is never kept twice.
 */
@Entity
@Table(name = "tape_preset")
@IdClass(OwnedKey.class)
public class TapePreset extends PanacheEntityBase
{

	@Id
	@JsonIgnore
	public String userId;

	/** For example "80-along". */
	@Id
	public String id;

	@Column(name = "length_mm")
	public int lengthMm;

	/** The text runs along the tape rather than across it. */
	@Column(name = "read_along")
	public boolean readAlong;

	static String idOf(int lengthMm, boolean readAlong)
	{
		return lengthMm + (readAlong ? "-along" : "-across");
	}
}
