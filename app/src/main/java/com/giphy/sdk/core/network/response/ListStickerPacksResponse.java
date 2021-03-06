/*
 * Created by Nima Khoshini on 10/24/17.
 * Copyright (c) 2017 Giphy Inc.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.giphy.sdk.core.network.response;

import com.giphy.sdk.core.models.Meta;
import com.giphy.sdk.core.models.Pagination;
import com.giphy.sdk.core.models.StickerPack;

import java.util.List;

public class ListStickerPacksResponse implements GenericResponse {
  private List<StickerPack> data;
  public Pagination pagination;
  public Meta meta;

  public List<StickerPack> getData() {
    return data;
  }

  public void setData(List<StickerPack> data) {
    this.data = data;
  }

  public Pagination getPagination() {
    return pagination;
  }

  public void setPagination(Pagination pagination) {
    this.pagination = pagination;
  }

  public Meta getMeta() {
    return meta;
  }

  public void setMeta(Meta meta) {
    this.meta = meta;
  }
}
