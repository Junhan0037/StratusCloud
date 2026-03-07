"use client";

import { FormEvent, useState } from "react";
import {
  ComputeImageResponse,
  ComputeImageStatus,
  ComputeOsType,
  createComputeImage,
  getAuthSession,
  listComputeImages
} from "@/shared/lib/api/client";

const osTypes: ComputeOsType[] = ["LINUX", "WINDOWS"];
const statuses: Array<ComputeImageStatus | ""> = ["", "ACTIVE", "DEPRECATED"];

export function ImageCatalogPanel() {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [name, setName] = useState("ubuntu-22-04");
  const [version, setVersion] = useState("2026.03");
  const [osType, setOsType] = useState<ComputeOsType>("LINUX");
  const [status, setStatus] = useState<ComputeImageStatus>("ACTIVE");
  const [tags, setTags] = useState("stable,lts");
  const [filterTag, setFilterTag] = useState("");
  const [images, setImages] = useState<ComputeImageResponse[]>([]);
  const [errorMessage, setErrorMessage] = useState("");

  const refreshImages = async () => {
    try {
      const latest = await listComputeImages({
        tenantId: tenantId.trim(),
        tag: filterTag.trim()
      });
      setImages(latest);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "이미지 조회에 실패했습니다.");
    }
  };

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage("");
    try {
      await createComputeImage(
        tenantId.trim(),
        name.trim(),
        version.trim(),
        osType,
        tags.split(",").map((value) => value.trim()).filter(Boolean),
        status
      );
      setName("");
      setVersion("");
      await refreshImages();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "이미지 등록에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>이미지 카탈로그</h2>
      <form onSubmit={handleCreate} style={{ display: "grid", gap: 10 }}>
        <input
          value={tenantId}
          onChange={(event) => setTenantId(event.target.value)}
          placeholder="Tenant ID(UUID)"
          required
          style={inputStyle}
        />
        <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Image Name" required style={inputStyle} />
        <input
          value={version}
          onChange={(event) => setVersion(event.target.value)}
          placeholder="Version"
          required
          style={inputStyle}
        />
        <select value={osType} onChange={(event) => setOsType(event.target.value as ComputeOsType)} style={inputStyle}>
          {osTypes.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
        <select value={status} onChange={(event) => setStatus(event.target.value as ComputeImageStatus)} style={inputStyle}>
          {statuses.filter(Boolean).map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
        <input
          value={tags}
          onChange={(event) => setTags(event.target.value)}
          placeholder="Tags (comma separated)"
          style={inputStyle}
        />
        <button type="submit" style={buttonStyle}>
          이미지 등록
        </button>
      </form>

      <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
        <input
          value={filterTag}
          onChange={(event) => setFilterTag(event.target.value)}
          placeholder="Filter Tag"
          style={{ ...inputStyle, flex: 1 }}
        />
        <button type="button" style={secondaryButtonStyle} onClick={() => void refreshImages()}>
          카탈로그 조회
        </button>
      </div>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {images.map((image) => (
          <li key={image.id} style={{ marginBottom: 8 }}>
            {image.name} {image.version} / {image.osType} / {image.status}
            <div style={subTextStyle}>{image.id.slice(0, 8)} · {image.tags.join(", ") || "no-tags"}</div>
          </li>
        ))}
      </ul>

      {errorMessage ? <p style={errorStyle}>{errorMessage}</p> : null}
    </section>
  );
}

const panelStyle: React.CSSProperties = {
  background: "#ffffff",
  borderRadius: 16,
  padding: 20,
  boxShadow: "0 10px 20px rgba(11, 34, 68, 0.08)"
};

const inputStyle: React.CSSProperties = {
  height: 40,
  border: "1px solid #d8e1ee",
  borderRadius: 10,
  padding: "0 12px"
};

const buttonStyle: React.CSSProperties = {
  height: 40,
  border: "none",
  borderRadius: 10,
  background: "#0f4dc2",
  color: "#ffffff",
  fontWeight: 600,
  cursor: "pointer"
};

const secondaryButtonStyle: React.CSSProperties = {
  height: 40,
  border: "1px solid #b5c5dc",
  borderRadius: 10,
  background: "#ffffff",
  color: "#183961",
  fontWeight: 600,
  cursor: "pointer",
  padding: "0 12px"
};

const subTextStyle: React.CSSProperties = {
  marginTop: 4,
  color: "#5d6b7d",
  fontSize: 12
};

const errorStyle: React.CSSProperties = {
  color: "#d22c2c",
  marginTop: 10,
  marginBottom: 0,
  fontSize: 13
};
