"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import type {
  Dispute,
  DisputeStatus,
  DisputeType,
  DisputePriority,
  DisputeFilters,
  CreateDisputeResponseRequest,
} from "@/types";
import * as disputesApi from "@/lib/api/disputes";

export interface DisputesViewModel {
  // State
  statusFilter: DisputeStatus | "ALL";
  typeFilter: DisputeType | "ALL";
  priorityFilter: DisputePriority | "ALL";
  selectedDisputeId: string | null;

  // Data
  disputes: Dispute[];
  selectedDispute: Dispute | null;
  isLoading: boolean;
  isLoadingDispute: boolean;
  error: Error | null;

  // Actions
  setStatusFilter: (status: DisputeStatus | "ALL") => void;
  setTypeFilter: (type: DisputeType | "ALL") => void;
  setPriorityFilter: (priority: DisputePriority | "ALL") => void;
  selectDispute: (disputeId: string | null) => void;
  respondToDispute: (data: CreateDisputeResponseRequest) => void;
  acceptResolution: () => void;
  proposeResolution: (resolution: string, refundAmount?: number) => void;
  isResponding: boolean;
  isAccepting: boolean;
  isProposing: boolean;
  refetch: () => void;
}

export function useDisputes(): DisputesViewModel {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<DisputeStatus | "ALL">("ALL");
  const [typeFilter, setTypeFilter] = useState<DisputeType | "ALL">("ALL");
  const [priorityFilter, setPriorityFilter] = useState<DisputePriority | "ALL">("ALL");
  const [selectedDisputeId, setSelectedDisputeId] = useState<string | null>(null);

  // Fetch disputes list from real API
  const {
    data: disputes,
    isLoading,
    error,
    refetch,
  } = useQuery<Dispute[], Error>({
    queryKey: ["seller-disputes", statusFilter, typeFilter, priorityFilter],
    queryFn: async () => {
      const filters: DisputeFilters = {};
      if (statusFilter !== "ALL") filters.status = statusFilter;
      if (typeFilter !== "ALL") filters.type = typeFilter;
      if (priorityFilter !== "ALL") filters.priority = priorityFilter;

      const response = await disputesApi.getDisputes(filters);
      if (response.success && response.data) {
        return response.data.items;
      }
      throw new Error(response.message || "Failed to load disputes");
    },
  });

  // Fetch single dispute from real API
  const {
    data: selectedDispute,
    isLoading: isLoadingDispute,
  } = useQuery<Dispute | null, Error>({
    queryKey: ["seller-dispute", selectedDisputeId],
    queryFn: async () => {
      if (!selectedDisputeId) return null;
      const response = await disputesApi.getDispute(selectedDisputeId);
      if (response.success && response.data) {
        return response.data;
      }
      throw new Error(response.message || "Failed to load dispute");
    },
    enabled: !!selectedDisputeId,
  });

  // Respond to dispute mutation
  const respondMutation = useMutation({
    mutationFn: (data: CreateDisputeResponseRequest) => {
      if (!selectedDisputeId) throw new Error("No dispute selected");
      return disputesApi.respondToDispute(selectedDisputeId, data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-disputes"] });
      queryClient.invalidateQueries({ queryKey: ["seller-dispute", selectedDisputeId] });
      toast.success("Response sent successfully");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to send response");
    },
  });

  // Accept resolution mutation
  const acceptMutation = useMutation({
    mutationFn: () => {
      if (!selectedDisputeId) throw new Error("No dispute selected");
      return disputesApi.acceptResolution(selectedDisputeId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-disputes"] });
      queryClient.invalidateQueries({ queryKey: ["seller-dispute", selectedDisputeId] });
      toast.success("Resolution accepted");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to accept resolution");
    },
  });

  // Propose resolution mutation
  const proposeMutation = useMutation({
    mutationFn: ({ resolution, refundAmount }: { resolution: string; refundAmount?: number }) => {
      if (!selectedDisputeId) throw new Error("No dispute selected");
      return disputesApi.proposeResolution(selectedDisputeId, resolution, refundAmount);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["seller-disputes"] });
      queryClient.invalidateQueries({ queryKey: ["seller-dispute", selectedDisputeId] });
      toast.success("Resolution proposed");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Failed to propose resolution");
    },
  });

  return {
    statusFilter,
    typeFilter,
    priorityFilter,
    selectedDisputeId,
    disputes: disputes ?? [],
    selectedDispute: selectedDispute ?? null,
    isLoading,
    isLoadingDispute,
    error: error as Error | null,
    setStatusFilter,
    setTypeFilter,
    setPriorityFilter,
    selectDispute: setSelectedDisputeId,
    respondToDispute: respondMutation.mutate,
    acceptResolution: () => acceptMutation.mutate(),
    proposeResolution: (resolution: string, refundAmount?: number) =>
      proposeMutation.mutate({ resolution, refundAmount }),
    isResponding: respondMutation.isPending,
    isAccepting: acceptMutation.isPending,
    isProposing: proposeMutation.isPending,
    refetch,
  };
}
