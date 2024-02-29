/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.mirasol.compiler.internal;

import com.io7m.mirasol.core.MiMapType;
import com.io7m.mirasol.core.MiPackageElementType;
import com.io7m.mirasol.core.MiPackageName;
import com.io7m.mirasol.core.MiPackageReference;
import com.io7m.mirasol.core.MiPackageType;
import com.io7m.mirasol.core.MiSimpleName;
import com.io7m.mirasol.core.MiTypeReference;
import com.io7m.mirasol.core.MiTypeType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A package.
 */

final class MiPackage implements MiPackageType
{
  private final MiPackageName name;
  private final HashMap<MiSimpleName, MiTypeType> types;
  private final HashMap<MiSimpleName, MiMapType> maps;
  private final ArrayList<MiPackageReference> imports;
  private String documentation;

  MiPackage(
    final MiPackageName inName)
  {
    this.name = Objects.requireNonNull(inName, "name");
    this.types = new HashMap<>();
    this.maps = new HashMap<>();
    this.imports = new ArrayList<MiPackageReference>();
    this.documentation = "";
  }

  void addImport(
    final MiPackageReference reference)
  {
    this.imports.add(reference);
  }

  void addType(
    final MiTypeType type)
  {
    this.types.put(type.name(), type);
  }

  void addMap(
    final MiMapType map)
  {
    this.maps.put(map.name(), map);
  }

  @Override
  public MiPackageName name()
  {
    return this.name;
  }

  @Override
  public List<MiPackageReference> imports()
  {
    return List.copyOf(this.imports);
  }

  @Override
  public Optional<MiTypeReference> type(
    final MiSimpleName typeName)
  {
    Objects.requireNonNull(typeName, "name");
    return Optional.ofNullable(this.types.get(typeName))
      .map(t -> new MiTypeReference(this.name, t));
  }

  @Override
  public Optional<MiPackageElementType> object(
    final MiSimpleName simpleName)
  {
    Objects.requireNonNull(simpleName, "simpleName");
    return Optional.ofNullable((MiPackageElementType) this.types.get(simpleName))
      .or(() -> Optional.ofNullable((MiPackageElementType) this.maps.get(simpleName)));
  }

  @Override
  public int size()
  {
    return this.types.size() + this.maps.size();
  }

  @Override
  public boolean isEmpty()
  {
    return this.types.isEmpty() && this.maps.isEmpty();
  }

  @Override
  public boolean contains(
    final Object o)
  {
    return this.types.containsValue(o) || this.maps.containsValue(o);
  }

  @Override
  public Iterator<MiPackageElementType> iterator()
  {
    return this.stream().iterator();
  }

  @Override
  public Stream<MiPackageElementType> stream()
  {
    final Stream<MiPackageElementType> typeStream =
      this.types.values()
        .stream()
        .sorted(Comparator.comparing(MiTypeType::name))
        .map(MiPackageElementType.class::cast);

    final Stream<MiPackageElementType> mapStream =
      this.maps.values()
        .stream()
        .sorted(Comparator.comparing(MiMapType::name))
        .map(MiPackageElementType.class::cast);

    return Stream.concat(typeStream, mapStream);
  }

  @Override
  public Object[] toArray()
  {
    return this.stream().toArray();
  }

  @Override
  public <T> T[] toArray(
    final T[] a)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(
    final MiPackageElementType element)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(
    final Object o)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(
    final Collection<?> c)
  {
    var exists = true;
    for (final var x : c) {
      exists = exists & this.contains(x);
    }
    return exists;
  }

  @Override
  public boolean addAll(
    final Collection<? extends MiPackageElementType> c)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(
    final Collection<?> c)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(
    final Collection<?> c)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String documentation()
  {
    return this.documentation;
  }

  void setDocumentation(
    final String value)
  {
    this.documentation =
      Objects.requireNonNull(value, "value");
  }
}
