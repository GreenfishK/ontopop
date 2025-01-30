package org.scify.jedai.datamodel;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.google.common.testing.EqualsTester;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Verifies the functionality of {@link EntityProfile}. */
class EntityProfileTest {
  /**
   * Used as an incrementing integer to make generated {@link #generateEP()} instances unique 
   * and yet have predictable values from one run to the next.
   */
  private static final AtomicInteger ENTITY_PROFILE_ID = new AtomicInteger();

  @Test
  void testEqualsAndHashCode() {
    // group 1: Randomly generated EntityProfile by itself

    // group 2: Test exact copy and copy with reversed attributes
    EntityProfile ep1 = generateEP();
    EntityProfile ep1Copy = copyEP(ep1);
    assert ep1 != ep1Copy;
    EntityProfile ep1CopyReversed = new EntityProfile(ep1.getEntityUrl());
    copyEPAttributesInReverse(ep1, ep1CopyReversed);

    // group 3: Test copy with extra attribute. This will show that instances with the same URL but
    // different attributes are unequal.
    EntityProfile ep1Similar = copyEP(ep1);
    ep1Similar.addAttribute("unequal_name", "unequal_value");

    // group 4: Test same attributes and a different URL cause instances to be unequal
    EntityProfile ep1DiffUrl = new EntityProfile("ep1DiffUrl: " + ep1.getEntityUrl());
    copyEPAttributes(ep1, ep1DiffUrl);

    // group 5: Empty EntityProfile
    EntityProfile epEmpty = new EntityProfile("");
    EntityProfile epEmptyCopy = copyEP(epEmpty);

    // group 6: Empty attributes
    EntityProfile epEmptyAttributes = new EntityProfile("empty_attributes");
    EntityProfile epEmptyAttributesCopy = copyEP(epEmptyAttributes);

    // group 7: Empty URL
    EntityProfile epEmptyUrl = new EntityProfile("");
    epEmptyUrl.addAttribute("empty_url_attr1_name", "empty_url_attr1_value");
    EntityProfile epEmptyUrlCopy = copyEP(epEmptyUrl);

    // Each equality group is equal to all others in that group and unequal to everything in all
    // other equality groups
    new EqualsTester()
        .addEqualityGroup(generateEP()) // group 1
        .addEqualityGroup(ep1, ep1Copy, ep1CopyReversed) // group 2
        .addEqualityGroup(ep1Similar) // group 3
        .addEqualityGroup(ep1DiffUrl) // group 4
        .addEqualityGroup(epEmpty, epEmptyCopy) // group 5
        .addEqualityGroup(epEmptyAttributes, epEmptyAttributesCopy) // group 6
        .addEqualityGroup(epEmptyUrl, epEmptyUrlCopy) // group 7
        .testEquals();
  }

  @Test
  void testGetProfileSize() {
    EntityProfile ep = new EntityProfile("testGetProfileSize");
    assertThat(ep.getProfileSize(), is(0));
    
    ep.addAttribute("name1", "value1");
    assertThat(ep.getProfileSize(), is(1));

    ep.addAttribute("name2", "value2");
    assertThat(ep.getProfileSize(), is(2));
  }
  
  @Test
  void testHashCodeEffectiveness() {
    EntityProfile ep1 = generateEP();
    EntityProfile ep2 = generateEP();
    // Strictly speaking, if the hash code is equal, it is not "incorrect". However, it may perform
    // poorly.
    assertThat(ep1.hashCode(), is(not(equalTo(ep2.hashCode()))));
  }

  /**
   * Helper method which takes an {@code EntityProfile} and generates a new {@code EntityProfile}
   * instance with an equal URL and equal set of attributes.
   *
   * @param ep the {@code EntityProfile} to copy
   * @return the copied {@code EntityProfile}.
   */
  private EntityProfile copyEP(EntityProfile ep) {
    requireNonNull(ep, "ep cannot be null");
    EntityProfile newEP = new EntityProfile(new String(ep.getEntityUrl()));
    copyEPAttributes(ep, newEP);
    return newEP;
  }

  /**
   * Deep copies the {@code Attributes} from the {@code from} {@code EntityProfile} to the {@code
   * to} {@code EntityProfile}.
   */
  private void copyEPAttributes(EntityProfile from, EntityProfile to) {
    requireNonNull(from, "from cannot be null");
    requireNonNull(to, "to cannot be null");
    for (Attribute attribute : from.getAttributes()) {
      to.addAttribute(new String(attribute.getName()), new String(attribute.getValue()));
    }
  }

  /**
   * Similar to {@link #copyEPAttributes(EntityProfile, EntityProfile)} but copies the attribute
   * list in reverse order. This shouldn't make any difference if the attribute container is a
   * {@code Set}, but this should expose a problem if it is changed to a collection implementation
   * where order matters.
   */
  private void copyEPAttributesInReverse(EntityProfile from, EntityProfile to) {
    requireNonNull(from, "from cannot be null");
    requireNonNull(to, "to cannot be null");
    //    from.getAttributes().
    List<Attribute> attributes = new ArrayList<>(from.getAttributes());
    ListIterator<Attribute> itr = attributes.listIterator(attributes.size());
    while (itr.hasPrevious()) {
      Attribute attribute = itr.previous();
      to.addAttribute(new String(attribute.getName()), new String(attribute.getValue()));
    }
  }

  /**
   * Helper method which creates a new {@code EntityProfile} with a URL based on 
   * {@link #ENTITY_PROFILE_ID} and a couple of attributes.
   */
  private EntityProfile generateEP() {
    String id = "id" + String.valueOf(ENTITY_PROFILE_ID.incrementAndGet());
    EntityProfile ep = new EntityProfile(id);
    ep.addAttribute(id + "_attr_name1", id + "_attr_value1");
    ep.addAttribute(id + "_attr_name2", id + "_attr_value1");
    return ep;
  }
}
