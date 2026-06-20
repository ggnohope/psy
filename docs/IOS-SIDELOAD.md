# Cài Psy lên iPhone (không cần Apple Developer paid)

App iOS phát hành ở GitHub Releases là **IPA chưa ký** (`Psy-<version>-unsigned.ipa`). iPhone không cài thẳng IPA chưa ký — bạn dùng một công cụ **tự ký bằng Apple ID free** của chính mình. Dưới đây là 3 cách, từ ít phiền tới nhiều phiền.

> **Giới hạn của Apple ID free (áp dụng cho mọi cách bên dưới):** app hết hạn **7 ngày** → phải *refresh* (ký lại); tối đa **3 app active** + **10 lượt ký/tuần**. Muốn hết các giới hạn này → Apple Developer paid + TestFlight.

Tải IPA: vào **Releases** của repo → mở bản `vX.Y.Z` → tải file `Psy-X.Y.Z-unsigned.ipa`.

---

## Cách 1 — SideStore (khuyên dùng: tự gia hạn, không cần máy tính sau khi setup)

Setup **một lần** cần một máy tính; sau đó iPhone **tự ký lại ngầm** mỗi 7 ngày.

1. Trên iPhone: cài **SideStore** theo hướng dẫn chính thức → https://sidestore.io (mục *Install*). Bạn sẽ:
   - Tạo **pairing file** (qua máy tính, 1 lần) và cài SideStore lên máy.
   - Bật **VPN của SideStore** (nó dùng VPN nội bộ để tự ký, KHÔNG phải VPN ra internet).
   - Đăng nhập **Apple ID free** trong SideStore.
2. Mở SideStore → tab **My Apps** → **+** → chọn file `Psy-X.Y.Z-unsigned.ipa` đã tải → đợi cài xong.
3. Lần đầu mở app: **Settings → General → VPN & Device Management** → tin tưởng (Trust) Apple ID của bạn.
4. **Gia hạn khi gần hết 7 ngày:** mở SideStore → **Refresh All** (1 chạm), hoặc để nó tự refresh ngầm (nên mở SideStore vài ngày/lần cho chắc). App **giữ nguyên dữ liệu**.

> Cập nhật bản mới: tải IPA mới từ Release rồi cài đè trong SideStore.

---

## Cách 2 — AltStore (giống SideStore nhưng cần máy tính cùng WiFi để refresh)

1. Cài **AltServer** trên máy tính (Win/Mac) + **AltStore** lên iPhone: https://altstore.io
2. Trong AltStore → **+** → chọn IPA `Psy-...unsigned.ipa`.
3. Tự refresh được, nhưng **máy tính chạy AltServer phải cùng mạng WiFi** với iPhone khi tới hạn. Tiện nếu máy tính luôn bật ở nhà.

---

## Cách 3 — Sideloadly (đơn giản nhất để cài, nhưng refresh thủ công)

1. Cài **Sideloadly** trên máy tính: https://sideloadly.io
2. Cắm iPhone → mở Sideloadly → kéo thả IPA vào → nhập Apple ID free → **Start**.
3. Hết 7 ngày: cắm lại máy + Start lại (thủ công). Không tự gia hạn.

---

## Gửi cho người thân
Gửi họ **link file IPA** (trong Release) + link trang này. Mỗi người tự cài bằng 1 trong 3 cách trên với **Apple ID free của họ** (mỗi người cần máy tính ít nhất lúc setup).

Nếu muốn người thân cài **dễ như App Store** (bấm link, không máy tính, không lo 7 ngày) → cần **Apple Developer paid ($99/năm)** rồi chuyển sang **TestFlight** (xem `docs/CICD.md` mục Release để đổi workflow).

---

## Vì sao KHÔNG dùng "dịch vụ ký online" lậu
Mấy trang bán/ký bằng cert lậu hay bị Apple **revoke** → app chết đồng loạt không báo trước; nhiều trang dính **malware / ăn cắp Apple ID** và vi phạm ToS. SideStore (ký bằng Apple ID free của chính bạn) làm đúng việc này **miễn phí và an toàn hơn**.
