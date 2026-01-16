"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  MessageSquareWarning,
  Search,
  Filter,
  ArrowLeft,
  Clock,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Send,
  Loader2,
  MessageCircle,
  Package,
  User,
  Calendar,
} from "lucide-react";
import { Header } from "@/components/layout/header";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { formatDateTime, formatRelativeTime, formatCurrency } from "@/lib/utils";
import { useDisputes } from "@/lib/hooks/useDisputes";
import type { DisputeStatus, DisputePriority, DisputeType, Dispute, DisputeMessage } from "@/types";

const statusFilters: { label: string; value: DisputeStatus | "ALL" }[] = [
  { label: "All", value: "ALL" },
  { label: "Open", value: "OPEN" },
  { label: "Awaiting Response", value: "WAITING_SELLER_RESPONSE" },
  { label: "Under Review", value: "UNDER_REVIEW" },
  { label: "Resolved", value: "RESOLVED" },
];

const statusConfig: Record<DisputeStatus, { color: string; icon: typeof Clock; label: string }> = {
  OPEN: { color: "bg-blue-100 text-blue-800", icon: Clock, label: "Open" },
  UNDER_REVIEW: { color: "bg-yellow-100 text-yellow-800", icon: Clock, label: "Under Review" },
  WAITING_SELLER_RESPONSE: { color: "bg-orange-100 text-orange-800", icon: AlertTriangle, label: "Awaiting Your Response" },
  WAITING_BUYER_RESPONSE: { color: "bg-purple-100 text-purple-800", icon: Clock, label: "Awaiting Buyer" },
  ESCALATED: { color: "bg-red-100 text-red-800", icon: AlertTriangle, label: "Escalated" },
  RESOLVED: { color: "bg-green-100 text-green-800", icon: CheckCircle, label: "Resolved" },
  CLOSED: { color: "bg-gray-100 text-gray-800", icon: XCircle, label: "Closed" },
};

const priorityConfig: Record<DisputePriority, { color: string; label: string }> = {
  LOW: { color: "bg-gray-100 text-gray-800", label: "Low" },
  MEDIUM: { color: "bg-blue-100 text-blue-800", label: "Medium" },
  HIGH: { color: "bg-orange-100 text-orange-800", label: "High" },
  URGENT: { color: "bg-red-100 text-red-800", label: "Urgent" },
};

const typeLabels: Record<DisputeType, string> = {
  QUALITY_ISSUE: "Quality Issue",
  QUANTITY_MISMATCH: "Quantity Mismatch",
  WRONG_PRODUCT: "Wrong Product",
  DAMAGED_PRODUCT: "Damaged Product",
  LATE_DELIVERY: "Late Delivery",
  NON_DELIVERY: "Non-Delivery",
  PAYMENT_ISSUE: "Payment Issue",
  OTHER: "Other",
};

const responseSchema = z.object({
  message: z.string().min(10, "Response must be at least 10 characters"),
  proposedResolution: z.string().optional(),
  refundAmount: z.number().min(0).optional(),
});

type ResponseFormData = z.infer<typeof responseSchema>;

export default function DisputesPage() {
  const vm = useDisputes();
  const [search, setSearch] = useState("");

  // Filter disputes by search
  const filteredDisputes = vm.disputes.filter((dispute) => {
    const matchesSearch =
      dispute.disputeNumber.toLowerCase().includes(search.toLowerCase()) ||
      dispute.orderNumber.toLowerCase().includes(search.toLowerCase()) ||
      dispute.buyerName.toLowerCase().includes(search.toLowerCase());
    return matchesSearch;
  });

  // If a dispute is selected, show detail view
  if (vm.selectedDisputeId && vm.selectedDispute) {
    return (
      <DisputeDetailView
        dispute={vm.selectedDispute}
        isLoading={vm.isLoadingDispute}
        onBack={() => vm.selectDispute(null)}
        onRespond={vm.respondToDispute}
        onAcceptResolution={vm.acceptResolution}
        onProposeResolution={vm.proposeResolution}
        isResponding={vm.isResponding}
        isAccepting={vm.isAccepting}
        isProposing={vm.isProposing}
      />
    );
  }

  return (
    <div>
      <Header title="Disputes" />

      <div className="p-6 space-y-6">
        {/* Filters */}
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative max-w-sm">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search disputes..."
              className="pl-10"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="flex flex-wrap gap-2">
            {statusFilters.map((filter) => (
              <Button
                key={filter.value}
                variant={vm.statusFilter === filter.value ? "default" : "outline"}
                size="sm"
                onClick={() => vm.setStatusFilter(filter.value)}
              >
                {filter.label}
              </Button>
            ))}
          </div>
        </div>

        {/* Loading state */}
        {vm.isLoading && (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {/* Error state */}
        {vm.error && (
          <Card>
            <CardContent className="p-8 text-center text-destructive">
              {vm.error.message || "Failed to load disputes"}
            </CardContent>
          </Card>
        )}

        {/* Disputes list */}
        {!vm.isLoading && !vm.error && (
          <div className="space-y-4">
            {filteredDisputes.map((dispute) => (
              <DisputeCard
                key={dispute.id}
                dispute={dispute}
                onClick={() => vm.selectDispute(dispute.id)}
              />
            ))}

            {filteredDisputes.length === 0 && (
              <Card>
                <CardContent className="p-8 text-center">
                  <MessageSquareWarning className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
                  <p className="text-muted-foreground">
                    {search
                      ? "No disputes found matching your search."
                      : "No disputes found. Disputes from buyers will appear here."}
                  </p>
                </CardContent>
              </Card>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function DisputeCard({ dispute, onClick }: { dispute: Dispute; onClick: () => void }) {
  const statusInfo = statusConfig[dispute.status];
  const priorityInfo = priorityConfig[dispute.priority];
  const StatusIcon = statusInfo.icon;

  return (
    <Card className="cursor-pointer hover:bg-muted/50 transition-colors" onClick={onClick}>
      <CardContent className="p-4">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-semibold">{dispute.disputeNumber}</span>
              <Badge className={statusInfo.color}>
                <StatusIcon className="mr-1 h-3 w-3" />
                {statusInfo.label}
              </Badge>
              <Badge className={priorityInfo.color}>{priorityInfo.label}</Badge>
            </div>
            <p className="text-sm font-medium">{typeLabels[dispute.type]}</p>
            <p className="text-sm text-muted-foreground line-clamp-2">
              {dispute.description}
            </p>
            <div className="flex flex-wrap items-center gap-4 text-xs text-muted-foreground">
              <span className="flex items-center gap-1">
                <Package className="h-3 w-3" />
                {dispute.orderNumber}
              </span>
              <span className="flex items-center gap-1">
                <User className="h-3 w-3" />
                {dispute.buyerName}
              </span>
              <span className="flex items-center gap-1">
                <Calendar className="h-3 w-3" />
                {formatRelativeTime(dispute.createdAt)}
              </span>
              {dispute.messages.length > 0 && (
                <span className="flex items-center gap-1">
                  <MessageCircle className="h-3 w-3" />
                  {dispute.messages.length} messages
                </span>
              )}
            </div>
          </div>

          <Button variant="outline" size="sm" className="shrink-0">
            View Details
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

interface DisputeDetailViewProps {
  dispute: Dispute;
  isLoading: boolean;
  onBack: () => void;
  onRespond: (data: { message: string }) => void;
  onAcceptResolution: () => void;
  onProposeResolution: (resolution: string, refundAmount?: number) => void;
  isResponding: boolean;
  isAccepting: boolean;
  isProposing: boolean;
}

function DisputeDetailView({
  dispute,
  isLoading,
  onBack,
  onRespond,
  onAcceptResolution,
  onProposeResolution,
  isResponding,
  isAccepting,
  isProposing,
}: DisputeDetailViewProps) {
  const [showResolutionForm, setShowResolutionForm] = useState(false);
  const statusInfo = statusConfig[dispute.status];
  const priorityInfo = priorityConfig[dispute.priority];
  const StatusIcon = statusInfo.icon;

  const form = useForm<ResponseFormData>({
    resolver: zodResolver(responseSchema),
    defaultValues: {
      message: "",
      proposedResolution: "",
      refundAmount: 0,
    },
  });

  const handleSubmitResponse = (data: ResponseFormData) => {
    onRespond({ message: data.message });
    form.reset();
  };

  const handleProposeResolution = (data: ResponseFormData) => {
    if (data.proposedResolution) {
      onProposeResolution(data.proposedResolution, data.refundAmount);
      form.reset();
      setShowResolutionForm(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  const canRespond = dispute.status === "WAITING_SELLER_RESPONSE" || dispute.status === "OPEN";
  const canProposeResolution = dispute.status !== "RESOLVED" && dispute.status !== "CLOSED";

  return (
    <div>
      <Header title="Dispute Details" />

      <div className="p-6 space-y-6">
        {/* Back button */}
        <Button variant="ghost" onClick={onBack} className="gap-2">
          <ArrowLeft className="h-4 w-4" />
          Back to Disputes
        </Button>

        <div className="grid gap-6 lg:grid-cols-3">
          {/* Main content */}
          <div className="lg:col-span-2 space-y-6">
            {/* Dispute Info */}
            <Card>
              <CardHeader>
                <div className="flex flex-wrap items-center gap-2 mb-2">
                  <CardTitle>{dispute.disputeNumber}</CardTitle>
                  <Badge className={statusInfo.color}>
                    <StatusIcon className="mr-1 h-3 w-3" />
                    {statusInfo.label}
                  </Badge>
                  <Badge className={priorityInfo.color}>{priorityInfo.label}</Badge>
                </div>
                <CardDescription>
                  Opened {formatDateTime(dispute.createdAt)}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <Label className="text-muted-foreground">Issue Type</Label>
                  <p className="font-medium">{typeLabels[dispute.type]}</p>
                </div>
                <div>
                  <Label className="text-muted-foreground">Description</Label>
                  <p>{dispute.description}</p>
                </div>
                <div>
                  <Label className="text-muted-foreground">Requested Resolution</Label>
                  <p>{dispute.requestedResolution}</p>
                </div>
                {dispute.resolution && (
                  <div className="p-4 bg-green-50 rounded-lg border border-green-200">
                    <Label className="text-green-800">Resolution</Label>
                    <p className="text-green-900">{dispute.resolution}</p>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Messages */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <MessageCircle className="h-5 w-5" />
                  Conversation
                </CardTitle>
              </CardHeader>
              <CardContent>
                {dispute.messages.length > 0 ? (
                  <div className="space-y-4">
                    {dispute.messages.map((message) => (
                      <MessageBubble key={message.id} message={message} />
                    ))}
                  </div>
                ) : (
                  <p className="text-center text-muted-foreground py-4">
                    No messages yet
                  </p>
                )}

                {/* Response Form */}
                {canRespond && (
                  <>
                    <Separator className="my-6" />
                    <form onSubmit={form.handleSubmit(handleSubmitResponse)} className="space-y-4">
                      <div className="space-y-2">
                        <Label htmlFor="message">Your Response</Label>
                        <Textarea
                          id="message"
                          {...form.register("message")}
                          placeholder="Type your response to the buyer..."
                          rows={4}
                        />
                        {form.formState.errors.message && (
                          <p className="text-sm text-destructive">
                            {form.formState.errors.message.message}
                          </p>
                        )}
                      </div>
                      <div className="flex justify-end gap-2">
                        {canProposeResolution && !showResolutionForm && (
                          <Button
                            type="button"
                            variant="outline"
                            onClick={() => setShowResolutionForm(true)}
                          >
                            Propose Resolution
                          </Button>
                        )}
                        <Button type="submit" disabled={isResponding}>
                          {isResponding ? (
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          ) : (
                            <Send className="mr-2 h-4 w-4" />
                          )}
                          Send Response
                        </Button>
                      </div>
                    </form>
                  </>
                )}

                {/* Resolution Form */}
                {showResolutionForm && (
                  <>
                    <Separator className="my-6" />
                    <form onSubmit={form.handleSubmit(handleProposeResolution)} className="space-y-4">
                      <div className="space-y-2">
                        <Label htmlFor="proposedResolution">Proposed Resolution</Label>
                        <Textarea
                          id="proposedResolution"
                          {...form.register("proposedResolution")}
                          placeholder="Describe your proposed resolution..."
                          rows={3}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor="refundAmount">Refund Amount (Optional)</Label>
                        <Input
                          id="refundAmount"
                          type="number"
                          {...form.register("refundAmount", { valueAsNumber: true })}
                          placeholder="0"
                        />
                      </div>
                      <div className="flex justify-end gap-2">
                        <Button
                          type="button"
                          variant="outline"
                          onClick={() => setShowResolutionForm(false)}
                        >
                          Cancel
                        </Button>
                        <Button type="submit" disabled={isProposing}>
                          {isProposing ? (
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          ) : (
                            <CheckCircle className="mr-2 h-4 w-4" />
                          )}
                          Submit Resolution
                        </Button>
                      </div>
                    </form>
                  </>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Order Info */}
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Order Information</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Order Number</span>
                  <span className="font-medium">{dispute.orderNumber}</span>
                </div>
                <Separator />
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Buyer</span>
                  <span className="font-medium">{dispute.buyerName}</span>
                </div>
              </CardContent>
            </Card>

            {/* Timeline */}
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Status Timeline</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <TimelineItem
                    title="Dispute Opened"
                    description={`By ${dispute.buyerName}`}
                    date={dispute.createdAt}
                    isComplete
                  />
                  {dispute.status !== "OPEN" && (
                    <TimelineItem
                      title="Under Review"
                      description="Being reviewed by platform"
                      date={dispute.updatedAt}
                      isComplete={dispute.status !== "UNDER_REVIEW"}
                    />
                  )}
                  {dispute.resolvedAt && (
                    <TimelineItem
                      title="Resolved"
                      description={`By ${dispute.resolvedBy}`}
                      date={dispute.resolvedAt}
                      isComplete
                    />
                  )}
                </div>
              </CardContent>
            </Card>

            {/* Actions */}
            {dispute.status === "WAITING_SELLER_RESPONSE" && (
              <Card className="border-orange-200 bg-orange-50">
                <CardHeader>
                  <CardTitle className="text-base text-orange-800 flex items-center gap-2">
                    <AlertTriangle className="h-4 w-4" />
                    Action Required
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-sm text-orange-700 mb-4">
                    Please respond to this dispute within 48 hours to avoid automatic escalation.
                  </p>
                </CardContent>
              </Card>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function MessageBubble({ message }: { message: DisputeMessage }) {
  const isSeller = message.senderType === "SELLER";
  const isAdmin = message.senderType === "ADMIN";

  return (
    <div className={`flex ${isSeller ? "justify-end" : "justify-start"}`}>
      <div
        className={`max-w-[80%] rounded-lg p-3 ${
          isSeller
            ? "bg-primary text-primary-foreground"
            : isAdmin
            ? "bg-purple-100 text-purple-900"
            : "bg-muted"
        }`}
      >
        <div className="flex items-center gap-2 mb-1">
          <span className="text-xs font-medium">
            {message.senderName}
          </span>
          {isAdmin && <Badge variant="secondary" className="text-[10px] px-1 py-0">Admin</Badge>}
        </div>
        <p className="text-sm">{message.message}</p>
        <p className={`text-[10px] mt-1 ${isSeller ? "text-primary-foreground/70" : "text-muted-foreground"}`}>
          {formatRelativeTime(message.createdAt)}
        </p>
      </div>
    </div>
  );
}

function TimelineItem({
  title,
  description,
  date,
  isComplete,
}: {
  title: string;
  description: string;
  date: string;
  isComplete: boolean;
}) {
  return (
    <div className="flex gap-3">
      <div className="flex flex-col items-center">
        <div
          className={`h-3 w-3 rounded-full ${
            isComplete ? "bg-green-500" : "bg-gray-300"
          }`}
        />
        <div className="w-0.5 flex-1 bg-gray-200" />
      </div>
      <div className="pb-4">
        <p className="text-sm font-medium">{title}</p>
        <p className="text-xs text-muted-foreground">{description}</p>
        <p className="text-xs text-muted-foreground">
          {formatRelativeTime(date)}
        </p>
      </div>
    </div>
  );
}
