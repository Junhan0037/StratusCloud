"use client";

import { ChangeEvent, FormEvent, useEffect, useState } from "react";
import {
  ProjectResponse,
  StorageBucketResponse,
  StorageObjectAcl,
  StorageObjectResponse,
  createStorageBucket,
  createStoragePresign,
  deleteStorageBucket,
  deleteStorageObject,
  getAuthSession,
  listStorageBuckets,
  listStorageObjects,
  resolveApiUrl,
  uploadStorageObject
} from "@/shared/lib/api/client";
import { buttonStyle, dangerButtonStyle, errorStyle, inputStyle, panelStyle, secondaryButtonStyle, subTextStyle } from "@/features/network/shared/panelStyles";

interface ObjectStoragePanelProps {
  selectedProject: ProjectResponse | null;
}

const aclOptions: StorageObjectAcl[] = ["PRIVATE", "PUBLIC_READ"];

export function ObjectStoragePanel({ selectedProject }: ObjectStoragePanelProps) {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [bucketName, setBucketName] = useState("artifact-bucket");
  const [bucketAcl, setBucketAcl] = useState<StorageObjectAcl>("PRIVATE");
  const [buckets, setBuckets] = useState<StorageBucketResponse[]>([]);
  const [selectedBucketId, setSelectedBucketId] = useState("");
  const [objects, setObjects] = useState<StorageObjectResponse[]>([]);
  const [objectKey, setObjectKey] = useState("release/app.txt");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [lastDownloadUrl, setLastDownloadUrl] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const refresh = async () => {
    if (!selectedProject) {
      setBuckets([]);
      setObjects([]);
      setSelectedBucketId("");
      return;
    }
    const latestBuckets = await listStorageBuckets(tenantId.trim(), selectedProject.id);
    setBuckets(latestBuckets);
    const nextBucketId = selectedBucketId && latestBuckets.some((bucket) => bucket.id === selectedBucketId)
      ? selectedBucketId
      : latestBuckets[0]?.id ?? "";
    setSelectedBucketId(nextBucketId);
    setObjects(nextBucketId ? await listStorageObjects(nextBucketId) : []);
  };

  useEffect(() => {
    const run = async () => {
      try {
        if (!selectedProject) {
          setBuckets([]);
          setObjects([]);
          setSelectedBucketId("");
          return;
        }
        const latestBuckets = await listStorageBuckets(tenantId.trim(), selectedProject.id);
        setBuckets(latestBuckets);
        const nextBucketId = selectedBucketId && latestBuckets.some((bucket) => bucket.id === selectedBucketId)
          ? selectedBucketId
          : latestBuckets[0]?.id ?? "";
        setSelectedBucketId(nextBucketId);
        setObjects(nextBucketId ? await listStorageObjects(nextBucketId) : []);
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "Storage 조회에 실패했습니다.");
      }
    };
    void run();
  }, [selectedProject, tenantId, selectedBucketId]);

  const handleCreateBucket = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedProject) {
      setErrorMessage("프로젝트를 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await createStorageBucket(tenantId.trim(), selectedProject.id, bucketName.trim(), bucketAcl);
      setBucketName("logs-bucket");
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "버킷 생성에 실패했습니다.");
    }
  };

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    setSelectedFile(event.target.files?.[0] ?? null);
  };

  const handleUpload = async () => {
    if (!selectedProject || !selectedBucketId || !selectedFile) {
      setErrorMessage("프로젝트, 버킷, 파일을 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      const presign = await createStoragePresign(
        tenantId.trim(),
        selectedProject.id,
        selectedBucketId,
        "UPLOAD",
        objectKey.trim(),
        selectedFile.type || "application/octet-stream"
      );
      await uploadStorageObject(presign.url, selectedFile);
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "오브젝트 업로드에 실패했습니다.");
    }
  };

  const handleGenerateDownload = async (key: string) => {
    if (!selectedProject || !selectedBucketId) {
      setErrorMessage("프로젝트와 버킷을 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      const presign = await createStoragePresign(
        tenantId.trim(),
        selectedProject.id,
        selectedBucketId,
        "DOWNLOAD",
        key
      );
      const absoluteUrl = resolveApiUrl(presign.url);
      setLastDownloadUrl(absoluteUrl);
      window.open(absoluteUrl, "_blank", "noopener,noreferrer");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "다운로드 URL 발급에 실패했습니다.");
    }
  };

  const handleDeleteObject = async (objectId: string) => {
    setErrorMessage("");
    try {
      await deleteStorageObject(objectId);
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "오브젝트 삭제에 실패했습니다.");
    }
  };

  const handleDeleteBucket = async () => {
    if (!selectedBucketId) {
      setErrorMessage("삭제할 버킷을 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await deleteStorageBucket(selectedBucketId);
      setLastDownloadUrl("");
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "버킷 삭제에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>Object Storage 운영</h2>
      <p style={subTextStyle}>
        선택된 프로젝트: {selectedProject ? `${selectedProject.name} (${selectedProject.id.slice(0, 8)})` : "없음"}
      </p>

      <form onSubmit={handleCreateBucket} style={{ display: "grid", gap: 10 }}>
        <input value={tenantId} onChange={(event) => setTenantId(event.target.value)} placeholder="Tenant ID(UUID)" required style={inputStyle} />
        <input value={bucketName} onChange={(event) => setBucketName(event.target.value)} placeholder="Bucket Name" required style={inputStyle} />
        <select value={bucketAcl} onChange={(event) => setBucketAcl(event.target.value as StorageObjectAcl)} style={inputStyle}>
          {aclOptions.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="submit" style={buttonStyle}>
            버킷 생성
          </button>
          <button type="button" style={secondaryButtonStyle} onClick={() => void refresh()}>
            버킷 조회
          </button>
        </div>
      </form>

      <div style={{ display: "grid", gap: 10, marginTop: 14 }}>
        <select value={selectedBucketId} onChange={(event) => setSelectedBucketId(event.target.value)} style={inputStyle}>
          <option value="">작업할 버킷 선택</option>
          {buckets.map((bucket) => (
            <option key={bucket.id} value={bucket.id}>
              {bucket.name} ({bucket.objectCount})
            </option>
          ))}
        </select>
        <input value={objectKey} onChange={(event) => setObjectKey(event.target.value)} placeholder="Object Key" style={inputStyle} />
        <input type="file" onChange={handleFileChange} style={{ ...inputStyle, paddingTop: 8 }} />
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="button" style={buttonStyle} onClick={() => void handleUpload()}>
            오브젝트 업로드
          </button>
          <button type="button" style={dangerButtonStyle} onClick={() => void handleDeleteBucket()}>
            빈 버킷 삭제
          </button>
        </div>
      </div>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {objects.map((object) => (
          <li key={object.id} style={{ marginBottom: 12 }}>
            <strong>{object.key}</strong> / {object.contentType} / {object.sizeBytes} bytes
            <div style={subTextStyle}>acl={object.acl} / etag={object.etag.slice(0, 12)}</div>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginTop: 6 }}>
              <button type="button" style={secondaryButtonStyle} onClick={() => void handleGenerateDownload(object.key)}>
                다운로드 URL 발급
              </button>
              <button type="button" style={dangerButtonStyle} onClick={() => void handleDeleteObject(object.id)}>
                Delete
              </button>
            </div>
          </li>
        ))}
      </ul>

      {lastDownloadUrl ? <p style={subTextStyle}>최근 다운로드 URL: {lastDownloadUrl}</p> : null}
      {errorMessage ? <p style={errorStyle}>{errorMessage}</p> : null}
    </section>
  );
}
