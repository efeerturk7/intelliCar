package com.efeerturk.intelliCar.dto.response;

import java.util.List;

/**
 * @param url      yüklenen ilk dosyanın tam adresi
 * @param imageUrl url ile aynı (CarImageRequest.imageUrl alanıyla aynı isim, doğrudan ilana eklemek için)
 * @param urls     birden fazla dosya yüklendiyse hepsinin adresleri
 */
public record FileUploadResponse(String url, String imageUrl, List<String> urls) {
}
