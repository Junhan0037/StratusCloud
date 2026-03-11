"use client";

import { ChangeEvent, FormEvent, useEffect, useState } from "react";
import {
  ProjectResponse,
  StorageBucketMeteringResponse,
  StorageBucketResponse,
  StorageGovernancePolicyResponse,
  StorageObjectAcl,
  StorageObjectResponse,
  StorageProjectMeteringResponse,
  createStorageBucket,
  createStoragePresign,
  deleteStorageBucket,
  deleteStorageObject,
  getAuthSession,
  getStorageBucketMetering,
  getStorageGovernancePolicy,
  getStorageProjectMetering,
  listStorageBucketTags,
  listStorageBuckets,
  listStorageObjectTags,
  listStorageObjects,
  resolveApiUrl,
  updateStorageBucketTags,
  updateStorageGovernancePolicy,
  updateStorageObjectTags,
  uploadStorageObject
} from "@/shared/lib/api/client";
import { buttonStyle, dangerButtonStyle, errorStyle, inputStyle, panelStyle, secondaryButtonStyle, subTextStyle } from "@/features/network/shared/panelStyles";

interface ObjectStoragePanelProps {
  selectedProject: ProjectResponse | null;
}

const aclOptions: StorageObjectAcl[] = ["PRIVATE", "PUBLIC_READ"];

const emptyPolicyForm = {
  maxBucketCount: "",
  maxObjectCount: "",
  maxTotalBytes: "",
  presignPerMinute: "",
  uploadPerMinute: "",
  downloadPerMinute: ""
};

function parseTagInput(value: string): string[] {
  return value.split(",").map((item) => item.trim()).filter(Boolean);
}

function toNullableNumber(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : null;
}

function formatPolicy(policy: StorageGovernancePolicyResponse | null) {
  return {
    maxBucketCount: policy?.maxBucketCount?.toString() ?? "",
    maxObjectCount: policy?.maxObjectCount?.toString() ?? "",
    maxTotalBytes: policy?.maxTotalBytes?.toString() ?? "",
    presignPerMinute: policy?.presignPerMinute?.toString() ?? "",
    uploadPerMinute: policy?.uploadPerMinute?.toString() ?? "",
    downloadPerMinute: policy?.downloadPerMinute?.toString() ?? ""
  };
}

export function ObjectStoragePanel({ selectedProject }: ObjectStoragePanelProps) {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [bucketName, setBucketName] = useState("artifact-bucket");
  const [bucketAcl, setBucketAcl] = useState<StorageObjectAcl>("PRIVATE");
  const [buckets, setBuckets] = useState<StorageBucketResponse[]>([]);
  const [selectedBucketId, setSelectedBucketId] = useState("");
  const [objects, setObjects] = useState<StorageObjectResponse[]>([]);
  const [bucketTagsInput, setBucketTagsInput] = useState("");
  const [objectTagInputs, setObjectTagInputs] = useState<Record<string, string>>({});
  const [objectKey, setObjectKey] = useState("release/app.txt");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [policyForm, setPolicyForm] = useState(emptyPolicyForm);
  const [policy, setPolicy] = useState<StorageGovernancePolicyResponse | null>(null);
  const [projectMetering, setProjectMetering] = useState<StorageProjectMeteringResponse | null>(null);
  const [bucketMetering, setBucketMetering] = useState<StorageBucketMeteringResponse | null>(null);
  const [lastDownloadUrl, setLastDownloadUrl] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const refresh = async () => {
    if (!selectedProject) {
      setBuckets([]);
      setObjects([]);
      setSelectedBucketId("");
      setPolicy(null);
      setPolicyForm(emptyPolicyForm);
      setProjectMetering(null);
      setBucketMetering(null);
      setBucketTagsInput("");
      setObjectTagInputs({});
      return;
    }

    const [latestBuckets, latestPolicy, latestProjectMetering] = await Promise.all([
      listStorageBuckets(tenantId.trim(), selectedProject.id),
      getStorageGovernancePolicy(selectedProject.id),
      getStorageProjectMetering(selectedProject.id)
    ]);

    setBuckets(latestBuckets);
    setPolicy(latestPolicy);
    setPolicyForm(formatPolicy(latestPolicy));
    setProjectMetering(latestProjectMetering);

    const nextBucketId = selectedBucketId && latestBuckets.some((bucket) => bucket.id === selectedBucketId)
      ? selectedBucketId
      : latestBuckets[0]?.id ?? "";
    setSelectedBucketId(nextBucketId);

    if (!nextBucketId) {
      setObjects([]);
      setBucketMetering(null);
      setBucketTagsInput("");
      setObjectTagInputs({});
      return;
    }

    const [latestObjects, latestBucketTags, latestBucketMetering] = await Promise.all([
      listStorageObjects(nextBucketId),
      listStorageBucketTags(nextBucketId),
      getStorageBucketMetering(nextBucketId)
    ]);
    setObjects(latestObjects);
    setBucketTagsInput(latestBucketTags.tags.join(", "));
    setBucketMetering(latestBucketMetering);

    const objectTagsEntries = await Promise.all(
      latestObjects.map(async (object) => {
        const response = await listStorageObjectTags(object.id);
        return [object.id, response.tags.join(", ")] as const;
      })
    );
    setObjectTagInputs(Object.fromEntries(objectTagsEntries));
  };

  useEffect(() => {
    const run = async () => {
      try {
        if (!selectedProject) {
          setBuckets([]);
          setObjects([]);
          setSelectedBucketId("");
          setPolicy(null);
          setPolicyForm(emptyPolicyForm);
          setProjectMetering(null);
          setBucketMetering(null);
          setBucketTagsInput("");
          setObjectTagInputs({});
          return;
        }

        const [latestBuckets, latestPolicy, latestProjectMetering] = await Promise.all([
          listStorageBuckets(tenantId.trim(), selectedProject.id),
          getStorageGovernancePolicy(selectedProject.id),
          getStorageProjectMetering(selectedProject.id)
        ]);

        setBuckets(latestBuckets);
        setPolicy(latestPolicy);
        setPolicyForm(formatPolicy(latestPolicy));
        setProjectMetering(latestProjectMetering);

        const nextBucketId = selectedBucketId && latestBuckets.some((bucket) => bucket.id === selectedBucketId)
          ? selectedBucketId
          : latestBuckets[0]?.id ?? "";
        setSelectedBucketId(nextBucketId);

        if (!nextBucketId) {
          setObjects([]);
          setBucketMetering(null);
          setBucketTagsInput("");
          setObjectTagInputs({});
          return;
        }

        const [latestObjects, latestBucketTags, latestBucketMetering] = await Promise.all([
          listStorageObjects(nextBucketId),
          listStorageBucketTags(nextBucketId),
          getStorageBucketMetering(nextBucketId)
        ]);
        setObjects(latestObjects);
        setBucketTagsInput(latestBucketTags.tags.join(", "));
        setBucketMetering(latestBucketMetering);

        const objectTagsEntries = await Promise.all(
          latestObjects.map(async (object) => {
            const response = await listStorageObjectTags(object.id);
            return [object.id, response.tags.join(", ")] as const;
          })
        );
        setObjectTagInputs(Object.fromEntries(objectTagsEntries));
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "Storage Governance 조회에 실패했습니다.");
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

  const handleSavePolicy = async () => {
    if (!selectedProject) {
      setErrorMessage("프로젝트를 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      const nextPolicy = await updateStorageGovernancePolicy(tenantId.trim(), selectedProject.id, {
        maxBucketCount: toNullableNumber(policyForm.maxBucketCount),
        maxObjectCount: toNullableNumber(policyForm.maxObjectCount),
        maxTotalBytes: toNullableNumber(policyForm.maxTotalBytes),
        presignPerMinute: toNullableNumber(policyForm.presignPerMinute),
        uploadPerMinute: toNullableNumber(policyForm.uploadPerMinute),
        downloadPerMinute: toNullableNumber(policyForm.downloadPerMinute)
      });
      setPolicy(nextPolicy);
      setPolicyForm(formatPolicy(nextPolicy));
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "정책 저장에 실패했습니다.");
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
      await refresh();
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

  const handleSaveBucketTags = async () => {
    if (!selectedBucketId) {
      setErrorMessage("버킷을 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      const response = await updateStorageBucketTags(selectedBucketId, parseTagInput(bucketTagsInput));
      setBucketTagsInput(response.tags.join(", "));
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "버킷 태그 저장에 실패했습니다.");
    }
  };

  const handleSaveObjectTags = async (objectId: string) => {
    setErrorMessage("");
    try {
      const response = await updateStorageObjectTags(objectId, parseTagInput(objectTagInputs[objectId] ?? ""));
      setObjectTagInputs((current) => ({
        ...current,
        [objectId]: response.tags.join(", ")
      }));
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "오브젝트 태그 저장에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>Object Storage + Governance</h2>
      <p style={subTextStyle}>
        선택된 프로젝트: {selectedProject ? `${selectedProject.name} (${selectedProject.id.slice(0, 8)})` : "없음"}
      </p>

      <div style={{ display: "grid", gap: 10 }}>
        <input value={tenantId} onChange={(event) => setTenantId(event.target.value)} placeholder="Tenant ID(UUID)" required style={inputStyle} />
        <div style={{ display: "grid", gap: 8, gridTemplateColumns: "repeat(2, minmax(0, 1fr))" }}>
          <input value={policyForm.maxBucketCount} onChange={(event) => setPolicyForm((current) => ({ ...current, maxBucketCount: event.target.value }))} placeholder="Max Bucket Count" style={inputStyle} />
          <input value={policyForm.maxObjectCount} onChange={(event) => setPolicyForm((current) => ({ ...current, maxObjectCount: event.target.value }))} placeholder="Max Object Count" style={inputStyle} />
          <input value={policyForm.maxTotalBytes} onChange={(event) => setPolicyForm((current) => ({ ...current, maxTotalBytes: event.target.value }))} placeholder="Max Total Bytes" style={inputStyle} />
          <input value={policyForm.presignPerMinute} onChange={(event) => setPolicyForm((current) => ({ ...current, presignPerMinute: event.target.value }))} placeholder="Presign / min" style={inputStyle} />
          <input value={policyForm.uploadPerMinute} onChange={(event) => setPolicyForm((current) => ({ ...current, uploadPerMinute: event.target.value }))} placeholder="Upload / min" style={inputStyle} />
          <input value={policyForm.downloadPerMinute} onChange={(event) => setPolicyForm((current) => ({ ...current, downloadPerMinute: event.target.value }))} placeholder="Download / min" style={inputStyle} />
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="button" style={buttonStyle} onClick={() => void handleSavePolicy()}>
            정책 저장
          </button>
          <button type="button" style={secondaryButtonStyle} onClick={() => void refresh()}>
            정책/미터링 새로고침
          </button>
        </div>
        <div style={subTextStyle}>
          프로젝트 미터링: bucket={projectMetering?.bucketCount ?? 0}, object={projectMetering?.objectCount ?? 0}, stored={projectMetering?.storedBytes ?? 0} bytes, uploaded={projectMetering?.uploadedBytes ?? 0}, downloaded={projectMetering?.downloadedBytes ?? 0}
        </div>
      </div>

      <form onSubmit={handleCreateBucket} style={{ display: "grid", gap: 10, marginTop: 16 }}>
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
        <input value={bucketTagsInput} onChange={(event) => setBucketTagsInput(event.target.value)} placeholder="Bucket Tags (comma separated)" style={inputStyle} />
        <div style={subTextStyle}>
          버킷 미터링: object={bucketMetering?.objectCount ?? 0}, stored={bucketMetering?.storedBytes ?? 0} bytes, uploaded={bucketMetering?.uploadedBytes ?? 0}, downloaded={bucketMetering?.downloadedBytes ?? 0}
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="button" style={secondaryButtonStyle} onClick={() => void handleSaveBucketTags()}>
            버킷 태그 저장
          </button>
          <button type="button" style={dangerButtonStyle} onClick={() => void handleDeleteBucket()}>
            빈 버킷 삭제
          </button>
        </div>
        <input value={objectKey} onChange={(event) => setObjectKey(event.target.value)} placeholder="Object Key" style={inputStyle} />
        <input type="file" onChange={handleFileChange} style={{ ...inputStyle, paddingTop: 8 }} />
        <button type="button" style={buttonStyle} onClick={() => void handleUpload()}>
          오브젝트 업로드
        </button>
      </div>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {objects.map((object) => (
          <li key={object.id} style={{ marginBottom: 12 }}>
            <strong>{object.key}</strong> / {object.contentType} / {object.sizeBytes} bytes
            <div style={subTextStyle}>acl={object.acl} / etag={object.etag.slice(0, 12)}</div>
            <input
              value={objectTagInputs[object.id] ?? ""}
              onChange={(event) => setObjectTagInputs((current) => ({ ...current, [object.id]: event.target.value }))}
              placeholder="Object Tags (comma separated)"
              style={{ ...inputStyle, marginTop: 8 }}
            />
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginTop: 6 }}>
              <button type="button" style={secondaryButtonStyle} onClick={() => void handleSaveObjectTags(object.id)}>
                태그 저장
              </button>
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
      {policy ? <p style={subTextStyle}>현재 정책 대상 프로젝트: {policy.projectId.slice(0, 8)}</p> : null}
      {errorMessage ? <p style={errorStyle}>{errorMessage}</p> : null}
    </section>
  );
}
